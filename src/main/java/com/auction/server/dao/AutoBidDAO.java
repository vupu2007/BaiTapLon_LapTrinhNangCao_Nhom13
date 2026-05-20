package com.auction.server.dao;

import com.auction.server.util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AutoBidDAO {

    // 1. Đăng ký auto-bid — nếu đã có thì cập nhật maxBid
    public boolean registerAutoBid(int auctionId, int bidderId, double maxBid) {
        if (hasAutoBid(auctionId, bidderId)) {
            return updateMaxBid(auctionId, bidderId, maxBid);
        }

        String sql = "INSERT INTO AutoBids (auction_id, bidder_id, max_bid) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, auctionId);
            pstmt.setInt(2, bidderId);
            pstmt.setDouble(3, maxBid);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 2. Lấy tất cả auto-bid của 1 phiên — maxBid cao nhất trước, đăng ký sớm nhất ưu tiên
    public List<int[]> getAutoBidsByAuction(int auctionId) {
        List<int[]> list = new ArrayList<>();
        String sql = "SELECT bidder_id, max_bid FROM AutoBids WHERE auction_id = ? ORDER BY max_bid DESC, created_at ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, auctionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new int[]{
                            rs.getInt("bidder_id"),
                            (int)(rs.getDouble("max_bid") * 100)
                    });
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // 3. Kiểm tra đã có auto-bid chưa
    public boolean hasAutoBid(int auctionId, int bidderId) {
        String sql = "SELECT auto_bid_id FROM AutoBids WHERE auction_id = ? AND bidder_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, auctionId);
            pstmt.setInt(2, bidderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 4. Cập nhật maxBid
    public boolean updateMaxBid(int auctionId, int bidderId, double newMaxBid) {
        String sql = "UPDATE AutoBids SET max_bid = ? WHERE auction_id = ? AND bidder_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, newMaxBid);
            pstmt.setInt(2, auctionId);
            pstmt.setInt(3, bidderId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 5. Xóa auto-bid khi phiên kết thúc
    public boolean deleteAutoBidsByAuction(int auctionId) {
        String sql = "DELETE FROM AutoBids WHERE auction_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, auctionId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    // Lất bid lơsn nhất
    public double getMaxBid(int auctionId, int bidderId) {
        String sql = "SELECT max_bid FROM AutoBids WHERE auction_id = ? AND bidder_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, auctionId);
            pstmt.setInt(2, bidderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getDouble("max_bid");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}