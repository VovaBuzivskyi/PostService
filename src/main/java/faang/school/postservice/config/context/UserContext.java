package faang.school.postservice.config.context;

import org.springframework.stereotype.Component;

@Component
public class UserContext {

    private static final ThreadLocal<Long> userIdHolder = new ThreadLocal<>();

    public static void setUserId(long userId) {
        userIdHolder.set(userId);
    }

    public static long getUserId() {
        return userIdHolder.get();
    }

    public void clear() {
        userIdHolder.remove();
    }
}
