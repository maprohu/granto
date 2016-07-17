package granto.testing.impl

import granto.testing.api.{GrantoTestingApi, Item}

/**
  * Created by martonpapp on 17/07/16.
  */
class GrantoTestingApiImpl extends GrantoTestingApi {

  override def canView(who: String, what: Item): Boolean = {
    what.size <= 5
  }

  override def close(): Unit = {}
}
