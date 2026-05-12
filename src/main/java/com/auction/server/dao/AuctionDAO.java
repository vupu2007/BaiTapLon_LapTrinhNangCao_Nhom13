package com.auction.server.dao;

import com.auction.shared.model.Auction;
import com.auction.shared.model.Auction.AuctionStatus;
import com.auction.shared.model.BidTransaction;
import com.auction.server.util.DatabaseConnection;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuctionDAO {

    // 1. Tạo phiên đấu giá mới
    public boolean insertAuction(Auction auction) {
        String sql = "INSERT INTO Auctions (item_id, seller_id, start_price, current_price, min_increment, start_time, end_time, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, auction.getItemId());
            pstmt.setInt(2, auction.getSellerId());
            pstmt.setDouble(3, auction.getStartPrice());
            pstmt.setDouble(4, auction.getStartPrice());
            pstmt.setDouble(5, auction.getMinIncrement());
            pstmt.setObject(6, auction.getStartTime());
            pstmt.setObject(7, auction.getEndTime());
            pstmt.setString(8, auction.getStatus().name());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 2. Lấy phiên đấu giá theo ID
    public Auction getAuctionById(int auctionId) {
        String sql = "SELECT * FROM Auctions WHERE auction_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, auctionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToAuction(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 3. Lấy tất cả phiên đấu giá
    public List<Auction> getAllAuctions() {
        List<Auction> list = new ArrayList<>();
        String sql = "SELECT * FROM Auctions";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapResultSetToAuction(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // 4. Lấy các phiên đấu giá theo trạng thái
    public List<Auction> getAuctionsByStatus(AuctionStatus status) {
        List<Auction> list = new ArrayList<>();
        String sql = "SELECT * FROM Auctions WHERE status = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status.name());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToAuction(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // 5. Lấy các phiên đấu giá của một Seller
    public List<Auction> getAuctionsBySeller(int sellerId) {
        List<Auction> list = new ArrayList<>();
        String sql = "SELECT * FROM Auctions WHERE seller_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, sellerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToAuction(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // THÊM: insertBid + updateCurrentPrice trong 1 transaction
    // → tránh lost update, tránh rollback giá, tránh 2 người cùng thắng
    public boolean placeBidTransaction(BidTransaction bid, double newPrice, int bidderId) {
        String insertBidSql    = "INSERT INTO Bids (auction_id, bidder_id, bid_amount) VALUES (?, ?, ?)";
        String updatePriceSql  = "UPDATE Auctions SET current_price = ?, winner_id = ? " +
                "WHERE auction_id = ? AND status = 'RUNNING' AND current_price < ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Bắt đầu transaction

            // Bước 1: Lưu bid
            try (PreparedStatement ps1 = conn.prepareStatement(insertBidSql)) {
                ps1.setInt(1, bid.getAuctionId());
                ps1.setInt(2, bid.getBidderId());
                ps1.setDouble(3, bid.getBidAmount());
                ps1.executeUpdate();
            }

            // Bước 2: Cập nhật giá — chỉ update nếu current_price < newPrice
            // → tránh trường hợp 2 thread cùng vào, thread sau ghi đè thread trước
            try (PreparedStatement ps2 = conn.prepareStatement(updatePriceSql)) {
                ps2.setDouble(1, newPrice);
                ps2.setInt(2, bidderId);
                ps2.setInt(3, bid.getAuctionId());
                ps2.setDouble(4, newPrice); // current_price < newPrice
                int rows = ps2.executeUpdate();
                if (rows == 0) {
                    // Có người khác vừa đặt giá cao hơn → rollback
                    conn.rollback();
                    System.err.println("Giá vừa bị vượt qua bởi người khác, vui lòng thử lại!");
                    return false;
                }
            }

            conn.commit(); // Hoàn tất transaction
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    // 6. Cập nhật trạng thái phiên đấu giá
    public boolean updateStatus(int auctionId, AuctionStatus newStatus) {
        String sql = "UPDATE Auctions SET status = ? WHERE auction_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newStatus.name());
            pstmt.setInt(2, auctionId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // --- Helper: map ResultSet sang Auction object ---
    private Auction mapResultSetToAuction(ResultSet rs) throws SQLException {
        Auction auction = new Auction();
        auction.setId(rs.getInt("auction_id"));
        auction.setItemId(rs.getString("item_id"));
        auction.setSellerId(rs.getInt("seller_id"));
        auction.setStartPrice(rs.getDouble("start_price"));
        auction.setCurrentPrice(rs.getDouble("current_price"));
        auction.setMinIncrement(rs.getDouble("min_increment"));
        auction.setStartTime(rs.getObject("start_time", LocalDateTime.class));
        auction.setEndTime(rs.getObject("end_time", LocalDateTime.class));
        auction.setStatus(AuctionStatus.valueOf(rs.getString("status")));

        int winnerId = rs.getInt("winner_id");
        if (!rs.wasNull()) auction.setWinnerId(winnerId);

        return auction;
    }
}