package com.auction.server.dao;

import com.auction.shared.model.BidTransaction;
import com.auction.server.util.DatabaseConnection;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BidDAO {

    // 1. Lưu một lần đặt giá mới vào DB
    public boolean insertBid(BidTransaction bid) {
        String sql = "INSERT INTO Bids (auction_id, bidder_id, bid_amount) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, bid.getAuctionId());
            pstmt.setInt(2, bid.getBidderId());
            pstmt.setDouble(3, bid.getBidAmount());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 2. Lấy toàn bộ lịch sử bid của một phiên đấu giá
    public List<BidTransaction> getBidsByAuction(int auctionId) {
        List<BidTransaction> list = new ArrayList<>();
        String sql = "SELECT * FROM Bids WHERE auction_id = ? ORDER BY bid_time ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, auctionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToBid(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // 3. Lấy bid cao nhất của một phiên (để kiểm tra trước khi cho đặt giá)
    public BidTransaction getHighestBid(int auctionId) {
        String sql = "SELECT * FROM Bids WHERE auction_id = ? ORDER BY bid_amount DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, auctionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToBid(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 4. Lấy lịch sử bid của một Bidder (xem mình đã đặt giá những đâu)
    public List<BidTransaction> getBidsByBidder(int bidderId) {
        List<BidTransaction> list = new ArrayList<>();
        String sql = "SELECT * FROM Bids WHERE bidder_id = ? ORDER BY bid_time DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, bidderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToBid(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // --- Helper: map ResultSet sang BidTransaction object ---
    private BidTransaction mapResultSetToBid(ResultSet rs) throws SQLException {
        BidTransaction bid = new BidTransaction();
        bid.setId(rs.getInt("bid_id"));
        bid.setAuctionId(rs.getInt("auction_id"));
        bid.setBidderId(rs.getInt("bidder_id"));
        bid.setBidAmount(rs.getDouble("bid_amount"));
        bid.setBidTime(rs.getObject("bid_time", LocalDateTime.class));
        return bid;
    }
}