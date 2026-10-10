package com.pmtool.security;

import com.pmtool.model.User;

public final class SecurityContext {

    private static final ThreadLocal<User> CURRENT_USER = new ThreadLocal<>();

    private SecurityContext() {}

    public static void setCurrentUser(User user) { CURRENT_USER.set(user); }
    public static User getCurrentUser() { return CURRENT_USER.get(); }
    public static void clear() { CURRENT_USER.remove(); }
}
