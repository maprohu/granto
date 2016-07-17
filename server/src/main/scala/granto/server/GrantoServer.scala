package granto.server

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.Tcp
import maprohu.scalaext.common.{Cancel, Stateful}

/**
  * Created by martonpapp on 17/07/16.
  */
object GrantoServer {

  def run(implicit
    actorSystem: ActorSystem,
    materializer: Materializer,
    config: GrantoServerConfig
  ) = {

    val cancels = Stateful.cancels

    Tcp().bind(
      "0.0.0.0",
      config.port
    ).mapMaterializedValue({ binding =>
      cancels.add(() => {
        Cancel(
          () => binding.flatMap(_.unbind())
        )
      })
    }).runForeach({ connection =>



    })

  }

}

trait GrantoServerConfig {

  def port : Int

}
