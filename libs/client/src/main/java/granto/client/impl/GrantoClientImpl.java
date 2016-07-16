package granto.client.impl;

import granto.client.GrantoApi;
import granto.client.GrantoClient;

/**
 * Created by martonpapp on 16/07/16.
 */
public class GrantoClientImpl implements GrantoClient {

    @Override
    public <T extends GrantoApi> T load(Class<T> clazz) {
        return null;
    }

}
