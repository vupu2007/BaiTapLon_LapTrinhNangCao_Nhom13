package com.auction.client.controller;

import com.auction.client.service.AccountService;
import com.auction.shared.network.Response;

public class AccountController {

    private final AccountService accountService;

    public AccountController() {
        this.accountService = new AccountService();
    }

    public Response loginUser(String username, String password) {
        System.out.println("[Controller] Yêu cầu đăng nhập: " + username);
        return accountService.loginUser(username, password);
    }

    public Response registerUser(String username, String password, String email) {
        System.out.println("[Controller] Yêu cầu đăng ký: " + username);
        return accountService.registerUser(username, password, email);
    }

    public Response forgotPassword(String username, String email) {
        System.out.println("[Controller] Yêu cầu quên mật khẩu: " + username);
        return accountService.forgotPassword(username, email);
    }
}