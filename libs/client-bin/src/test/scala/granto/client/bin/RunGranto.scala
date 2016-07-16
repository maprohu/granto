package granto.client.bin

/**
  * Created by martonpapp on 16/07/16.
  */
object RunGranto {

  def main(args: Array[String]) {

    val g = new GrantoClientImpl
    val api = g.load(classOf[TestingApi])
    println(api.booo("asfd"))
    println(api.toString)
    println(api.hashCode())
    println(api.equals(api))
    println(api.equals(new Object))

  }

}
