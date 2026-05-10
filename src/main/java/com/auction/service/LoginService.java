package com.auction.service;

import com.auction.model.UserStore;

public class LoginService {
    public boolean login(String user, String pass) {
        if (user == null || pass == null) return false;
        user = user.trim();
        return UserStore.users.containsKey(user)
                && UserStore.users.get(user).equals(pass);
    }
}