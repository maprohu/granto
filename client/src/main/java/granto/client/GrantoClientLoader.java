package granto.client;

import sun.misc.Service;

/**
 * Created by martonpapp on 16/07/16.
 */
public class GrantoClientLoader {

    public static GrantoClient newInstance() {
        return (GrantoClient) Service.providers(GrantoClient.class).next();
    }

}
