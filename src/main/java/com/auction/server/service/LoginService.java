package com.auction.server.service;

import com.auction.shared.model.UserStore;

public class LoginService {
    public boolean login(String user, String pass) {
        if (user == null || pass == null) return false;
        user = user.trim();
        return UserStore.users.containsKey(user)
                && UserStore.users.get(user).equals(pass);
    }
}