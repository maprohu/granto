package granto.client.bin

import java.io.File
import java.lang.reflect.{InvocationHandler, Method}
import java.util.Collections

import akka.actor.{ActorRef, ActorSystem, Status}
import akka.stream.{ActorMaterializer, OverflowStrategy, ThrottleMode}
import akka.stream.scaladsl.{Keep, Sink, Source, Tcp}
import ch.qos.logback.classic.LoggerContext
import com.typesafe.config.ConfigFactory
import granto.client.{GrantoAccessDenied, GrantoApi, GrantoClient, GrantoClientFactory}
import granto.lib.shared._
import maprohu.scalaext.common.{Cancel, Stateful}
import org.slf4j.LoggerFactory
import osgi6.actor.ActorSystemActivator
import osgi6.akka.slf4j.AkkaSlf4j
import osgi6.akka.stream.{Materializers, Stages}
import osgi6.common.ParentLastURLClassLoader
import osgi6.logback.Configurator
import sbt.io.IO

import scala.util.Properties
import sbt.io.Path._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future, Promise}
import scala.collection.immutable._

/**
  * Created by martonpapp on 16/07/16.
  */

class GrantoClientFactoryImpl extends GrantoClientFactory {
  override def newInstance(uniqueId: String): GrantoClient = {
    new GrantoClientImpl(uniqueId)
  }
}

class GrantoClientImpl(uniqueId: String) extends GrantoClient {

  val storage = new File(
    Properties.propOrNone(
      GrantoClient.storagePropertyName
    ).getOrElse(
      "target/granto-client"
    )
  ) / uniqueId

  val debug = Properties
    .propOrNone(GrantoClient.debugPropertyName)
    .map(_.toBoolean)
    .getOrElse(false)

  val delegate : GrantoClient =
    Properties.propOrNone(GrantoClient.urlPropertyName)
      .map({ urls =>

        val endpoints =
          urls
            .trim
            .split(',')
            .to[Seq]
            .map(_.trim)
            .filterNot(_.isEmpty)
            .map({ url =>
              val Array(host, port) = url.split(':')

              Endpoint(
                host,
                port.toInt
              )
            })

        GrantoClientImpl.run(uniqueId, endpoints, storage, debug)

      }).getOrElse({
        GrantAll
      })

  override def load[T <: GrantoApi](clazz: Class[T]): T = {
    delegate.load(clazz)
  }

  override def close(): Unit = {
    delegate.close()
  }

}

object GrantoClientImpl {

  def run(
    uniqueId: String,
    endpoints: Seq[Endpoint],
    storage: File,
    debug: Boolean
  ) : GrantoClient = {
    val lc = LoggerFactory.getILoggerFactory().asInstanceOf[LoggerContext]
    Configurator.configure(lc)(
      storage / "logs",
      "granto-client",
      false,
      debug
    )

    implicit val actorSystem = ActorSystem(
      s"granto-client-${uniqueId}",
      AkkaSlf4j.config.withFallback(
        ActorSystemActivator.forcedConfig.withFallback(
          ConfigFactory.parseURL(getClass.getClassLoader.getResource("reference.conf"))
        )
      ),
      getClass.getClassLoader
    )
    implicit val materializer = Materializers.resume
    import actorSystem.dispatcher

    val subscriptions = Stateful(Map[String, Seq[Subscription]]().withDefaultValue(Seq()))

    val outRef = Stateful(Option.empty[ActorRef])

    val outgoingCancels = Stateful.cancels

    val (connectStop, connectDone) = Source(Stream.continually(endpoints).flatten)
      .throttle(1, 3.second, 1, ThrottleMode.Shaping)
      .viaMat(
        Stages.stopper
      )(Keep.right)
      .mapAsync(1)({ endpoint =>

        outgoingCancels.add({ () =>
          val (ref, done) = subscriptions.process({ subs =>
             Source.actorRef[ClientToServer](256, OverflowStrategy.dropHead)
              .prepend(
                Source(subs.keys.map(Request(_)).to[Iterable])
              )
              .via(
                Protocol.stack[ServerToClient, ClientToServer]
                  .join(
                    Tcp().outgoingConnection(
                      endpoint.host,
                      endpoint.port
                    )
                  )
              )
              .toMat(
                Sink.foreach({
                  case m : Initial =>
                    subscriptions.extract(m.className).foreach(
                      _.initial(m)
                    )
                  case m : Update =>
                    subscriptions.extract(m.className).foreach(
                      _.update(m)
                    )
                  case Tick() =>
                  case Error(msg) =>
                    actorSystem.log.error("granto server error: {}", msg)

                })
              )(Keep.both)
              .run()
          })

          outRef.update(_ => Some(Some(ref)))


          Cancel(
            () => ref ! Status.Success,
            done.recover({
              case ex : Throwable =>
                actorSystem.log.error(ex, "disconnected from granto server")
            })
          )

        }).map(_.done).getOrElse(
          Future.successful()
        )

      })
      .toMat(
        Sink.ignore
      )(Keep.both)
      .run()

    new GrantoClient {
      override def load[T <: GrantoApi](clazz: Class[T]): T = {
        val className = clazz.getName
        val promise = Promise[GrantoApiWrapper[T]]()

        val delegate = Stateful(Option.empty[GrantoApiWrapper[T]])

        val sub = Subscription(
          className,
          { initial =>
            delegate.update({ dopt =>
              dopt
                .map({ d =>
                  d.setDelegate(loadBinary[T](initial.implementation))
                  None
                })
                .getOrElse({
                  val d = loadBinary[GrantoApiWrapper[T]](initial.delegate)
                  d.setDelegate(
                    loadBinary[T](initial.implementation)
                  )
                  promise.trySuccess(d)
                  Some(Some(d))
                })
            })
          },
          { update =>
            delegate.extract.get.setDelegate(
              loadBinary[T](update.update)
            )
          }

        )

        subscriptions.update({ map =>
          Some(
            map.updated(className, map(className) :+ sub)
          )
        })

        outRef.extract.foreach(_ ! Request(className))

        val del = Await.result(promise.future, 1.minute)

        del.setCloseCallback({ () =>
          subscriptions.update({ map =>
            Some(
              map.updated(className, map(className) diff Seq(sub))
            )
          })
        })

        del.wrapper()
      }

      override def close(): Unit = {
        connectStop()

        Await.ready(
          for {
            _ <- outgoingCancels.cancel.perform
            _ <- connectDone
          } yield {
          },
          15.seconds
        )

        actorSystem.shutdown()
        actorSystem.awaitTermination(10.seconds)
      }
    }

  }

  def loadBinary[T](
    binary: Binary
  ) : T = {
    val jar = File.createTempFile(binary.className, ".jar")
    IO.write(jar, binary.jar)
    val classLoader = new ParentLastURLClassLoader(Collections.singletonList(jar.toURI.toURL))
    classLoader.loadClass(binary.className).newInstance().asInstanceOf[T]
  }

}

case class Subscription(
  className: String,
  initial: Initial => Unit,
  update: Update => Unit
)

case class Endpoint(
  host: String,
  port: Int
)

class GrantProxy(fn: Method => AnyRef) extends GrantoClient {
  override def load[T <: GrantoApi](clazz: Class[T]): T = {
    val handler = new InvocationHandler {
      override def invoke(o: scala.AnyRef, method: Method, objects: Array[AnyRef]): AnyRef = {
        if (method.getDeclaringClass == classOf[Object]) {
          method.getName match {
            case "equals" =>
              (o eq objects(0)).asInstanceOf[AnyRef]
            case "hashCode" =>
              (System.identityHashCode(o)).asInstanceOf[AnyRef]
            case "toString" =>
              Integer.toHexString(System.identityHashCode(o)) +
                ", with InvocationHandler " + this;
          }
        } else {
          fn(method)
        }
      }
    }
    java.lang.reflect.Proxy.newProxyInstance(
      getClass.getClassLoader,
      Array( clazz ),
      handler
    ).asInstanceOf[T]
  }

  override def close(): Unit = {}
}

object GrantAll extends GrantProxy(_ => true.asInstanceOf[AnyRef])
object GrantNone extends GrantProxy({ method =>
  if (method.getReturnType == Void.TYPE) {
    throw new GrantoAccessDenied(s"${getClass.getName} in effect")
  } else {
    false.asInstanceOf[AnyRef]
  }
})
