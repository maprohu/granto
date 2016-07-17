package granto.testing.wrapper

import granto.client.bin.{GrantoApiWrapper, GrantoApiWrapperImpl, GrantoApiWrapperImplDelegated, GrantoDelegatedApiImpl}
import granto.testing.api.{GrantoTestingApi, Item}

/**
  * Created by martonpapp on 17/07/16.
  */
class GrantoTestingWrapper extends GrantoApiWrapperImplDelegated[GrantoTestingApi, GrantoTestingWrapper.type](GrantoTestingWrapper)

object GrantoTestingWrapper extends GrantoDelegatedApiImpl[GrantoTestingApi] with GrantoTestingApi {

  override def canView(who: String, what: Item): Boolean = delegate.canView(who, what)

}
