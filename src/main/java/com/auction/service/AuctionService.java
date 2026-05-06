package com.auction.service;

import com.auction.model.Bidder;
import com.auction.model.Item;
import com.auction.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AuctionService {
    // Các thuộc tính cũ có thể giữ lại nếu bạn muốn dùng làm cache
    private String auctionId;
    private Item auctionItem;
    private double currentHighestBid;
    private Bidder highestBidder;
    private boolean isRunning;

    // 1. THÊM MỚI: Constructor không tham số để hết báo đỏ ở Controller
    public AuctionService() {
        this.isRunning = true;
    }

    // 2. GIỮ LẠI: Constructor cũ (nếu cần dùng ở chỗ khác)
    public AuctionService(String auctionId, Item auctionItem) {
        this.auctionId = auctionId;
        this.auctionItem = auctionItem;
        this.currentHighestBid = (auctionItem != null) ? auctionItem.getStartingPrice() : 0;
        this.isRunning = true;
    }

    /**
     * Sửa hàm placeBid để vừa cập nhật RAM vừa lưu vào MySQL
     */
    public synchronized boolean placeBid(Bidder bidder, double bidAmount) {
        if (!isRunning) {
            System.out.println("Phiên đã đóng!");
            return false;
        }

        // Kiểm tra giá đặt (logic này sau này sẽ SELECT từ DB để check chính xác hơn)
        if (bidAmount <= currentHighestBid) {
            System.out.println("Giá đặt phải cao hơn giá hiện tại!");
            return false;
        }

        // Bước A: Lưu vào Database (MySQL)
        String sql = "INSERT INTO Bids (user_id, bid_amount, bid_time) VALUES (?, ?, NOW())";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, Integer.parseInt(bidder.getId()));
            pstmt.setDouble(2, bidAmount);
            pstmt.executeUpdate();

            // Bước B: Cập nhật lại bộ nhớ RAM để hiển thị nhanh
            this.currentHighestBid = bidAmount;
            this.highestBidder = bidder;

            System.out.println("Lưu DB & RAM thành công: " + bidder.getUsername());
            return true;

        } catch (SQLException e) {
            System.err.println("Lỗi kết nối MySQL khi đặt giá: " + e.getMessage());
            return false;
        } catch (NumberFormatException e) {
            System.err.println("Lỗi định dạng ID người dùng.");
            return false;
        }
    }

    public void closeAuction() {
        this.isRunning = false;
        System.out.println("Auction kết thúc!");
        // Sau này có thể thêm lệnh UPDATE trạng thái phiên đấu giá trong Database ở đây
    }

    // Các hàm getter giữ nguyên
    public double getCurrentHighestBid() { return currentHighestBid; }
    public Bidder getHighestBidder() { return highestBidder; }
    public boolean isRunning() { return isRunning; }
}