package granto.client;

import sun.misc.Service;

/**
 * Created by martonpapp on 16/07/16.
 */
public interface GrantoClient {

    public <T extends GrantoApi> T load(Class<T> clazz);

}
