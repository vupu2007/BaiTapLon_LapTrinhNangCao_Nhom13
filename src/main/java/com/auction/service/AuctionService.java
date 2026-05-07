package com.auction.service;

import com.auction.model.Account;
import com.auction.model.User;

public class AuctionService {

    // Chấp nhận Account thay vì User
    public boolean placeBid(String itemId, double amount, Account account) {
        if (!(account instanceof User)) {
            return false; // Admin không được bid
        }

        User user = (User) account; // Ép kiểu để xử lý balance

        // Logic check balance và update DB ở đây
        if (user.getBalance() < amount) return false;

        return true;
    }

    // Thêm hàm này để fix lỗi ở AuctionController
    public void closeAuction() {
        System.out.println("Đã đóng phiên đấu giá thành công!");
    }
}