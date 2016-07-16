package granto.client.impl;

import granto.client.GrantoClient;
import granto.client.GrantoClientFactory;

/**
 * Created by martonpapp on 16/07/16.
 */
public class GrantoClientFactoryImpl implements GrantoClientFactory {
    @Override
    public GrantoClient newInstance(String uniqueId) {
        return new GrantoClientImpl(uniqueId);
    }
}
