package granto.server

import akka.actor.{ActorSystem, Status}
import akka.stream.{Materializer, OverflowStrategy}
import akka.stream.hack.HackSource
import akka.stream.scaladsl._
import granto.lib.shared._
import maprohu.scalaext.common.{Cancel, Stateful}

import scala.concurrent.Future


/**
  * Created by martonpapp on 17/07/16.
  */
object GrantoServer {

  type RunningUpdater = (String, Initial) => Unit

  case class DataItem(
    initial: () => Initial,
    update: Initial => Unit,
    cancel: Cancel,
    updates: Source[Update, Any]
  )

  def run(implicit
    actorSystem: ActorSystem,
    materializer: Materializer,
    config: GrantoServerConfig
  ) : (RunningUpdater, Cancel) = {
    import actorSystem.dispatcher

    val cancels = Stateful.cancels

    val data = Stateful(Map[String, DataItem]())

    val bindCancel = runData(
      { request =>
        val item = data.extract(request.className)

        (item.initial(), item.updates)
      }
    )

    val runningUpdater : RunningUpdater = { (className, initial) =>

      data.update({ map =>
        map.get(className)
          .map({ item =>
            item.update(initial)
            None
          })
          .getOrElse({

            cancels.addValue({ () =>

              val (add, pub) = Source.actorRef[Update](16, OverflowStrategy.dropHead)
                .toMat(
                  Sink.asPublisher(true)
                )(Keep.both)
                .run()

              val done = Source.fromPublisher(pub)
                .toMat(Sink.ignore)(Keep.right)
                .run()

              val c = Cancel(
                () => add ! Status.Success,
                done
              )

              val initialHolder = Stateful(initial)

              val m = map.updated(
                className,
                DataItem(
                  () => initialHolder.extract,
                  { update =>
                    initialHolder.update(_ => Some(update))
                    add ! Update(
                      update.implementation
                    )
                  },
                  c,
                  Source.fromPublisher(pub)

                )
              )

              (c, m)
            })
          })
      })
    }

    val cancel = Cancel(
      () => {
        for {
          _ <- bindCancel.perform
          _ <- cancels.cancel.perform
        } yield ()
      }
    )

    (runningUpdater, cancel)
  }



  def runData(
    data: Request => (Initial, Source[Update, Any])
  )(implicit
    actorSystem: ActorSystem,
    materializer: Materializer,
    config: GrantoServerConfig
  ) : Cancel = {
    import actorSystem.dispatcher

    val cancels = Stateful.cancels

    Tcp().bind(
      "0.0.0.0",
      config.port
    ).toMat(
      Sink.foreach({ connection =>

        Flow[ClientToServer]
          .collect({ case m : Request => m })
          .flatMapConcat({ r =>
            try {
              val (init, updates) = data(r)

              Source.single(init)
                .concat(updates)
            } catch {
              case e : Exception =>
                actorSystem.log.error(e, "error processing request")
                Source.single(
                  Error(e.getMessage)
                )
            }
          })
          .merge(
            Source.tick(Messages.TickInterval, Messages.TickInterval, Tick)
          )
          .join(
            Protocol.stack[ClientToServer, ServerToClient]
          )
          .join(
            connection.flow
          )
          .run()

      })
    )({ (binding, done) =>
      cancels.add(() => {
        Cancel(
          () => binding.flatMap({ bound =>
            Future.sequence(
              Seq(
                bound.unbind(),
                done
              )
            )
          })
        )
      }).getOrElse(
        binding.foreach(_.unbind())
      )
    })

    cancels.cancel

  }

}

case class GrantoServerConfig(
  port : Int
)


