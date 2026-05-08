package com.auction.service;

import com.auction.model.UserStore;

public class RegisterService {

    public boolean register(String username, String password, String confirm) {
        if (username == null || password == null || confirm == null) return false;

        username = username.trim();

        if (username.isEmpty() || password.isEmpty()) return false;

        if (UserStore.users.containsKey(username)) return false;

        if (!password.equals(confirm)) return false;

        UserStore.users.put(username, password);
        return true;
    }
}