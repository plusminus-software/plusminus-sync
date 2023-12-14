package software.plusminus.sync.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import javax.servlet.http.HttpServletRequest;

@Component
public class DehydrationContext {

    public static final ThreadLocal<Boolean> DEHYDRATE = ThreadLocal.withInitial(() -> false);

    @Autowired
    private HttpServletRequest request;

    public boolean shouldDehydrate() {
        if (!isDehydrationEnabled()) {
            return false;
        }
        return DEHYDRATE.get();
    }

    public void runWithDehydration(Runnable runnable) {
        if (!isDehydrationEnabled()) {
            runnable.run();
            return;
        }
        try {
            DEHYDRATE.set(true);
            runnable.run();
        } finally {
            DEHYDRATE.set(false);
        }
    }

    private boolean isDehydrationEnabled() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return false;
        }
        String attribute = request.getParameter("dehydrate");
        return attribute != null && attribute.equalsIgnoreCase("true");
    }
}
