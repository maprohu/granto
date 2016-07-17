package granto.client.bin

import granto.client.GrantoClient

/**
  * Created by martonpapp on 16/07/16.
  */
object RunGranto {

  def main(args: Array[String]) {

    System.setProperty(GrantoClient.urlPropertyName, "localhost:9889")

    val g = new GrantoClientImpl("test")
    val api = g.load(classOf[TestingApi])
    println(api.booo("asfd"))
    println(api.toString)
    println(api.hashCode())
    println(api.equals(api))
    println(api.equals(new Object))

  }

}
