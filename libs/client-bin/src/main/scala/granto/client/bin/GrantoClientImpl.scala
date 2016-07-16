package granto.client.bin

import java.lang.reflect.{InvocationHandler, Method}

import granto.client.{GrantoApi, GrantoClient}

import scala.util.Properties

/**
  * Created by martonpapp on 16/07/16.
  */
class GrantoClientImpl extends GrantoClient {

  val delegate : GrantoClient =
    Properties.propOrNone(GrantoClient.urlPropertyName)
      .map({ url =>
        ???
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

object GrantAll extends GrantoClient {
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
          true.asInstanceOf[AnyRef]
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
