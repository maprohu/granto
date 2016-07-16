package granto.client.bin

import granto.client.GrantoApi

/**
  * Created by martonpapp on 16/07/16.
  */
trait TestingApi extends GrantoApi {

  def booo(in: String) : Boolean

}
