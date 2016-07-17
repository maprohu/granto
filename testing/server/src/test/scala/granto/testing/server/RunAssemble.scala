package granto.testing.server

import granto.testing.impl.GrantoTestingApiImpl
import granto.testing.wrapper.GrantoTestingWrapper

/**
  * Created by martonpapp on 17/07/16.
  */
object RunAssemble {

  def main(args: Array[String]) {

    val i = new GrantoTestingApiImpl
    val w = new GrantoTestingWrapper
    w.setDelegate(i)


  }

}
