package granto.client;

import sun.misc.Service;

/**
 * Created by martonpapp on 16/07/16.
 */
public interface GrantoClient {

    String urlPropertyName = "granto.url";
    String storagePropertyName = "granto.storage";
    String debugPropertyName = "granto.debug";

    <T extends GrantoApi> T load(Class<T> clazz);

    void close();

}
