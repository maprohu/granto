package granto.client.bin

import java.io.File
import java.lang.reflect.{InvocationHandler, Method}

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Keep, Sink, Source}
import ch.qos.logback.classic.LoggerContext
import com.typesafe.config.ConfigFactory
import granto.client.{GrantoAccessDenied, GrantoApi, GrantoClient, GrantoClientFactory}
import granto.lib.shared._
import maprohu.scalaext.common.Stateful
import org.slf4j.LoggerFactory
import osgi6.actor.ActorSystemActivator
import osgi6.akka.slf4j.AkkaSlf4j
import osgi6.akka.stream.Materializers
import osgi6.logback.Configurator

import scala.util.Properties
import sbt.io.Path._

import scala.concurrent.duration._
import scala.concurrent.{Await, Promise}

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
          ConfigFactory.parseURL(getClass.getResource("/reference.conf"))
        )
      )
    )
    implicit val materializer = Materializers.resume

    val subscriptions = Stateful(Map[String, Seq[Subscription]]().withDefaultValue(Seq()))

    val (ref, pub) = Source.actorRef[ServerToClient](1024, OverflowStrategy.dropHead)
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
          case Tick =>
          case Error(msg) =>
            actorSystem.log.error("granto server error: {}", msg)

        })
      )(Keep.both)
      .run()

    Source(Stream.continually(endpoints).flatten)
      .mapAsync(1)({ endpoint =>

      })
      .toMat(
        Sink.ignore
      )(Keep.right)
      .run()

    new GrantoClient {
      override def load[T <: GrantoApi](clazz: Class[T]): T = {
        val className = clazz.getName
        val promise = Promise[Initial]()

        val sub = Subscription(
          className,
          { initial =>
            
          }

        )

        Source.fromPublisher(pub)
          .collect({
            case m : Initial if m.className == className =>

          })


        Await.result(promise.future, 1.minute)

      }

      override def close(): Unit = ???
    }

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
