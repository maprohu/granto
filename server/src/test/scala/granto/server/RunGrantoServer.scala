package granto.server

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

/**
  * Created by martonpapp on 17/07/16.
  */
object RunGrantoServer {

  def main(args: Array[String]) {
    implicit val actorSystem = ActorSystem("test", ConfigFactory.parseString(
      """
        |akka.loglevel = "DEBUG"
      """.stripMargin).withFallback(ConfigFactory.load()))
    implicit val materializer = ActorMaterializer()

    implicit val config = GrantoServerConfig(9889)


    GrantoServer.run



  }

}
