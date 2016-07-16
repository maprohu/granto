package granto.client;

/**
 * Created by martonpapp on 16/07/16.
 */
public interface GrantoClientFactory {

    GrantoClient newInstance(String uniqueId);

}
