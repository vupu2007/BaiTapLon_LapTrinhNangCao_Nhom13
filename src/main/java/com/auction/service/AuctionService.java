package com.auction.service;

import com.auction.dao.ItemDAO;
import com.auction.model.Bidder;
import com.auction.model.Item;
import java.util.List;

public class AuctionService {
    // 1. Gọi ItemDAO để xử lý Database chuyên nghiệp
    private ItemDAO itemDAO = new ItemDAO();

    private boolean isRunning;

    public AuctionService() {
        this.isRunning = true;
    }

    /**
     * Hàm đặt giá thầu (Logic chính của bạn)
     */
    public synchronized String handlePlaceBid(String itemId, double bidAmount, Bidder bidder) {
        if (!isRunning) return "Phiên đấu giá đã kết thúc!";

        // Bước 1: Lấy thông tin mới nhất từ DB (Không dùng giá cũ trong RAM để so sánh)
        Item item = itemDAO.getItemById(itemId);
        if (item == null) return "Sản phẩm không tồn tại!";

        // Bước 2: Kiểm tra giá (Logic nghiệp vụ)
        if (bidAmount <= item.getCurrentPrice()) {
            return "Giá đặt phải cao hơn giá hiện tại: " + item.getCurrentPrice();
        }

        // Bước 3: Kiểm tra người bán (Không cho tự đấu giá đồ của mình)
        // Lưu ý: bidder.getId() là String nên cần ép kiểu nếu ownerId trong DB là int
        if (item.getOwnerId() == Integer.parseInt(bidder.getId())) {
            return "Bạn không thể đấu giá sản phẩm của chính mình!";
        }

        // Bước 4: Thực thi lưu vào Database
        // Gọi hàm updateCurrentPrice mà bạn vừa viết xong ở ItemDAO
        boolean success = itemDAO.updateCurrentPrice(itemId, bidAmount);

        if (success) {
            return "Đặt giá thành công! Bạn đang dẫn đầu.";
        } else {
            return "Lỗi hệ thống khi cập nhật giá.";
        }
    }

    // Các hàm bổ trợ cho giao diện
    public List<Item> getAuctionList() {
        return itemDAO.getAllItems();
    }

    public void closeAuction() {
        this.isRunning = false;
    }
}