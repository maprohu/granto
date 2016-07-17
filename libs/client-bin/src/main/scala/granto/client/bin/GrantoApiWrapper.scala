package granto.client.bin

import granto.client.GrantoApi

import scala.util.Try

/**
  * Created by martonpapp on 17/07/16.
  */
trait GrantoApiWrapper[T <: GrantoApi] {

  def wrapper() : T

  def setDelegate(delegate: T) : Unit

  def setCloseCallback(callback: () => Unit) : Unit

}

class GrantoApiWrapperImpl[T <: GrantoApi](
  _wrapper: T,
  _setDelegate: T => Unit,
  _setCloseCallback: (() => Unit) => Unit

) extends GrantoApiWrapper[T] {

  var delegateOpt : Option[T] = None

  override def wrapper(): T = _wrapper


  override def setCloseCallback(callback: () => Unit): Unit = {
    _setCloseCallback(callback)
  }

  override def setDelegate(delegate: T): Unit = synchronized {
    Try(delegateOpt.foreach(_.close()))
    delegateOpt = Option(delegate)
    _setDelegate(delegate)
  }

}

class GrantoApiWrapperImplDelegated[A <: GrantoApi, T <: GrantoDelegatedApiImpl[A] with A](
  _wrapper: T
) extends GrantoApiWrapperImpl[A](
  _wrapper,
  d => _wrapper.setDelegate(d),
  d => _wrapper.setCloseCallback(d)
)


class GrantoDelegatedApiImpl[T <: GrantoApi] extends GrantoApi {

  @volatile var delegateOpt : Option[T] = None

  var closeCallback : () => Unit = () => ()

  def delegate : T = delegateOpt.get

  def setDelegate(d: T) = synchronized {
    delegateOpt = Some(d)
  }

  def setCloseCallback(callback: () => Unit): Unit = {
    closeCallback = callback
  }

  override def close() : Unit = synchronized {
    Try(delegateOpt.foreach(_.close()))
    closeCallback()
    delegateOpt = None
  }


}


