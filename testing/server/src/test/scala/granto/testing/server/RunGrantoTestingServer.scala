package granto.testing.server

import java.io.File

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import granto.lib.shared.{Binary, Initial}
import granto.server.{GrantoServer, GrantoServerConfig}
import sbt.io.IO

/**
  * Created by martonpapp on 17/07/16.
  */
object RunGrantoTestingServer {

  def main(args: Array[String]) {
    implicit val actorSystem = ActorSystem("test", ConfigFactory.parseString(
      """
        |akka.loglevel = "DEBUG"
      """.stripMargin).withFallback(ConfigFactory.load()))
    implicit val materializer = ActorMaterializer()

    implicit val config = GrantoServerConfig(9889)


    val (running, cancel) = GrantoServer.run

    val initial =
      Initial(
        delegate = Binary(
          IO.readBytes(new File("testing/wrapper/target/wrapper.jar")),
          "granto.testing.wrapper.GrantoTestingWrapper"
        ),
        implementation = Binary(
          IO.readBytes(new File("testing/impl/target/impl.jar")),
          "granto.testing.impl.GrantoTestingApiImpl"
        )
      )


    running(
      "granto.testing.api.GrantoTestingApi",
      initial
    )
  }

}
