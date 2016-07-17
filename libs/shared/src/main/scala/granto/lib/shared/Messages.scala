package granto.lib.shared

import akka.actor.ActorSystem
import akka.serialization.{SerializationExtension, Serializer}
import akka.stream.io.Framing
import akka.stream.scaladsl.{BidiFlow, Flow}
import akka.util.ByteString

/**
  * Created by martonpapp on 17/07/16.
  */

import scala.concurrent.duration._

object Messages {

  val TickInterval = 15.seconds

}


case class Message[T](
  msg: T
)

sealed trait ClientToServer
sealed trait ServerToClient

object Tick extends ClientToServer with ServerToClient

case class Request(
  className: String
) extends ClientToServer

case class Binary(
  jar: Array[Byte],
  className: String
)

case class Initial(
  className: String,
  delegate: Binary,
  implementation: Binary
) extends ServerToClient

case class Update(
  className: String,
  update: Binary
) extends ServerToClient

case class Error(
  message: String
) extends ServerToClient


object Protocol {

  val framing = Framing.simpleFramingProtocol(Int.MaxValue / 2)

  def serializing[In <: AnyRef, Out <: AnyRef](
    serializer: Serializer
  ): BidiFlow[Out, ByteString, ByteString, In, Any] =
    BidiFlow.fromFlows(
      Flow[Out]
        .map(msg =>
          ByteString(serializer.toBinary(msg))
        ),
      Flow[ByteString]
        .map( bytes =>
          serializer
            .fromBinary(bytes.toArray)
            .asInstanceOf[In]
        )
    )



  def stack[In, Out](implicit
    actorSystem: ActorSystem
  ) : BidiFlow[Out, ByteString, ByteString, In, Any] = {
    val serialization = SerializationExtension(actorSystem)
    val serializer = serialization.serializerFor(classOf[Message[Any]])

    val wrapping =
      BidiFlow.fromFlows(
        Flow[Out]
          .map(Message(_)),
        Flow[Message[In]]
          .map(_.msg)
      )

    wrapping atop
      serializing[Message[In], Message[Out]](
        serializer
      ) atop
      framing
  }





}