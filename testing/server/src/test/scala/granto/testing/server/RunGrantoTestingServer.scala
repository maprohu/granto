package granto.testing.server

import java.io.File

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import granto.lib.shared.{Binary, Initial}
import granto.server.{GrantoServer, GrantoServerConfig}
import granto.testing.api.GrantoTestingApi
import granto.testing.impl.GrantoTestingApiImpl
import granto.testing.wrapper.GrantoTestingWrapper
import sbt.io.IO

import scala.io.StdIn

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

    val className =
      classOf[GrantoTestingApi].getName

    val (running, cancel) = GrantoServer.run

    val initial =
      Initial(
        className,
        delegate = Binary(
          IO.readBytes(new File("testing/wrapper/target/wrapper.jar")),
          classOf[GrantoTestingWrapper].getName
        ),
        implementation = Binary(
          IO.readBytes(new File("testing/impl/target/impl.jar")),
          classOf[GrantoTestingApiImpl].getName
        )
      )


    running(
      initial
    )

    while (true) {
      StdIn.readLine()

      val initial2 =
        Initial(
          className,
          delegate = Binary(
            IO.readBytes(new File("testing/wrapper/target/wrapper.jar")),
            classOf[GrantoTestingWrapper].getName
          ),
          implementation = Binary(
            IO.readBytes(new File("testing/impl2/target/impl2.jar")),
            classOf[GrantoTestingApiImpl].getName
          )
        )


      running(
        initial2
      )

      println("updated")


    }
  }

}
