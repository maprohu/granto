package granto.testing.api

import granto.client.GrantoApi

/**
  * Created by martonpapp on 16/07/16.
  */
trait GrantoTestingApi extends GrantoApi {

  def canView(who: String, what: Item) : Boolean

}

trait Item {
  def color : String
  def size : Int
}