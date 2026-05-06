package com.auction.controller;

import com.auction.model.User;
import com.auction.service.UserService;

public class UserController {

    private UserService userService = new UserService();

    public User loginUser(String username, String password) {
        System.out.println("Controller nhận request login");

        return userService.login(username, password);
    }

    public boolean registerUser(String username, String password, String role) {
        System.out.println("Controller nhận request register");

        return userService.register(username, password, role);
    }
}