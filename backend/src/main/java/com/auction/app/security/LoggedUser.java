package com.auction.app.security;

import com.auction.app.domains.users.users.User;

public class LoggedUser {
    private static User current;

    public static void set(User user) {
        current = user;
    }

    public static User get() {
        return current;
    }

    public static void clear() {
        current = null;
    }
}
