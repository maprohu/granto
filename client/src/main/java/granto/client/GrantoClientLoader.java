package granto.client;

import sun.misc.Service;

/**
 * Created by martonpapp on 16/07/16.
 */
public class GrantoClientLoader {

    public static GrantoClientFactory load() {
        return (GrantoClientFactory) Service.providers(GrantoClientFactory.class).next();
    }

}
