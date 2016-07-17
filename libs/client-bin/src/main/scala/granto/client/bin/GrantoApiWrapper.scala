package granto.client.bin

import granto.client.GrantoApi

import scala.util.Try

/**
  * Created by martonpapp on 17/07/16.
  */
trait GrantoApiWrapper[T <: GrantoApi] {

  def wrapper() : T

  def setDelegate(delegate: T) : Unit

}

class GrantoApiWrapperImpl[T <: GrantoApi](
  _wrapper: T,
  _setDelegate: T => Unit
) extends GrantoApiWrapper[T] {

  var delegateOpt : Option[T] = None

  override def wrapper(): T = _wrapper

  override def setDelegate(delegate: T): Unit = synchronized {
    Try(delegateOpt.foreach(_.close()))
    delegateOpt = Option(delegate)
    _setDelegate(delegate)
  }

}

class GrantoApiWrapperImplDelegated[A <: GrantoApi, T <: GrantoDelegatedApiImpl[A] with A](
  _wrapper: T
) extends GrantoApiWrapperImpl[T](
  _wrapper,
  d => _wrapper.setDelegate(d)
)


class GrantoDelegatedApiImpl[T <: GrantoApi] extends GrantoApi {

  @volatile var delegateOpt : Option[T] = None

  def delegate : T = delegateOpt.get

  def setDelegate(d: T) = synchronized {
    delegateOpt = Some(d)
  }

  override def close() : Unit = synchronized {
    Try(delegateOpt.foreach(_.close()))
    delegateOpt = None
  }


}


