package granto.client;

/**
 * Created by martonpapp on 16/07/16.
 */
public interface GrantoApi<User, Action, Resource> {

    boolean isGranted(User user, Action action, Resource resource);
    void require(User user, Action action, Resource resource) throws GrantoAccessDenied;

    void close();

}
