package granto.testing.app

import granto.client.{GrantoClient, GrantoClientLoader}
import granto.testing.api.{GrantoTestingApi, Item}

/**
  * Created by martonpapp on 16/07/16.
  */
object GrantoTestingApp {

  def main(args: Array[String]) {

    System.setProperty(GrantoClient.urlPropertyName, "localhost:9889")

    val granto = GrantoClientLoader.load().newInstance("testing")

    val api = granto.load(classOf[GrantoTestingApi])

    def result = api.canView(
      "john",
      new Item {
        override def color: String = "blue"
        override def size: Int = 42
      }
    )

    while (true) {
      println(result)
      Thread.sleep(300)

    }

  }

}
