package granto.testing.app

import granto.client.GrantoClientLoader
import granto.testing.api.{GrantoTestingApi, Item}

/**
  * Created by martonpapp on 16/07/16.
  */
object GrantoTestingApp {

  def main(args: Array[String]) {

    val granto = GrantoClientLoader.newInstance()

    val api = granto.load(classOf[GrantoTestingApi])

    val result = api.canView(
      "john",
      new Item {
        override def color: String = "blue"
        override def size: Int = 42
      }
    )

    println(result)
  }

}
