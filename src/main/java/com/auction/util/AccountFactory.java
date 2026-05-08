package com.auction.util;

import com.auction.model.Account;
import com.auction.model.Admin;
import com.auction.model.Bidder;
import com.auction.model.Seller;

public class AccountFactory {

    // Tạo đúng loại Account dựa theo role
    public static Account createAccount(int id, String username, String password, String email, String role) {
        switch (role.toUpperCase()) {
            case "ADMIN":
                return new Admin(String.valueOf(id), username, password, email);
            case "SELLER":
                return new Seller(String.valueOf(id), username, password, email, 0.0);
            default:
                return new Bidder(String.valueOf(id), username, password, email, 0.0);
        }
    }
}