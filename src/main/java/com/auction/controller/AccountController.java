package com.auction.controller;

import com.auction.model.Account;
import com.auction.model.User;
import com.auction.service.AccountService;

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