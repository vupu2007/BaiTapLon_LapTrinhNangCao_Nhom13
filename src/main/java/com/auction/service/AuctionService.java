package com.auction.service;

import com.auction.dao.ItemDAO;
import com.auction.model.Item;
import com.auction.model.User; // Sử dụng lớp cha User
import java.util.List;

public class AuctionService {
    private ItemDAO itemDAO = new ItemDAO();
    private boolean isRunning;

    public AuctionService() {
        this.isRunning = true;
    }

    /**
     * Logic đặt giá: Cho phép mọi User đặt giá miễn là không phải chủ sở hữu món đồ
     */
    public synchronized boolean placeBid(String itemId, double bidAmount, User user) {
        if (!isRunning) return false;

        // 1. Lấy thông tin món hàng từ DB
        Item item = itemDAO.getItemById(itemId);
        if (item == null) return false;

        // 2. Kiểm tra giá đặt phải cao hơn giá hiện tại
        if (bidAmount <= item.getCurrentPrice()) {
            return false;
        }

        // 3. LOGIC QUAN TRỌNG: Kiểm tra quyền sở hữu
        // Một người bán (Seller) vẫn có thể đặt giá nếu món đồ này KHÔNG thuộc về họ
        if (item.getOwnerId() == Integer.parseInt(user.getId())) {
            return false; // Chặn nếu tự đấu giá đồ của chính mình
        }

        // 4. Cập nhật vào Database
        return itemDAO.updateCurrentPrice(itemId, bidAmount);
    }

    public List<Item> getAuctionList() {
        return itemDAO.getAllItems();
    }

    public void closeAuction() {
        this.isRunning = false;
    }
}