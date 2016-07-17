package granto.client;

import java.util.Locale;

/**
 * Created by martonpapp on 17/07/16.
 */
public class GrantoAccessDenied extends RuntimeException {
    public GrantoAccessDenied() {
    }

    public GrantoAccessDenied(String s) {
        super(s);
    }

    public GrantoAccessDenied(String s, Throwable throwable) {
        super(s, throwable);
    }

    public GrantoAccessDenied(Throwable throwable) {
        super(throwable);
    }

    public String getReason(Locale locale) {
        return getMessage();
    }
}
