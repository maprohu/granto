package granto.testing.app

import granto.client.GrantoClientLoader
import granto.testing.api.GrantoTestingApi

/**
  * Created by martonpapp on 16/07/16.
  */
object GrantoTestingApp {

  def main(args: Array[String]) {

    val granto = GrantoClientLoader.newInstance()

    granto.load(classOf[GrantoTestingApi])


  }

}
