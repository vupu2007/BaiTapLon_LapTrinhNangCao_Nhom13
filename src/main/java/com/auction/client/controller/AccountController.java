package com.auction.client.controller;

import com.auction.shared.model.Account;
import com.auction.shared.model.User;
import com.auction.server.service.AccountService;

public class AccountController {

    private AccountService accountService = new AccountService();

    public Account loginUser(String username, String password) {
        System.out.println("Controller nhận request login");

        return accountService.login(username, password);
    }

    public boolean registerUser(String username, String password, String role) {
        System.out.println("Controller nhận request register");

        return accountService.register(username, password, role);
    }
}