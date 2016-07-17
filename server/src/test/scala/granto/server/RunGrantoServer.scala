package granto.server

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

/**
  * Created by martonpapp on 17/07/16.
  */
object RunGrantoServer {

  def main(args: Array[String]) {

    implicit val actorSystem = ActorSystem()
    implicit val materializer = ActorMaterializer()





  }

}
