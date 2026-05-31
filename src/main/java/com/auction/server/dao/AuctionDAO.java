package com.auction.server.dao;

import com.auction.shared.model.Auction;
import com.auction.shared.model.Auction.AuctionStatus;
import com.auction.shared.model.BidTransaction;
import com.auction.server.util.DatabaseConnection;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuctionDAO {

    // 1. Tạo phiên đấu giá mới (Lưu thêm original_end_time)
    public boolean insertAuction(Auction auction) {
        String sql = "INSERT INTO Auctions (item_id, seller_id, start_price, current_price, min_increment, start_time, end_time, original_end_time, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // 🛠️ ĐÃ SỬA: Ép kiểu sang dữ liệu Int để khớp cột INT trong DB
            pstmt.setInt(1, Integer.parseInt(auction.getItemId()));

            pstmt.setInt(2, auction.getSellerId());
            pstmt.setDouble(3, auction.getStartPrice());
            pstmt.setDouble(4, auction.getStartPrice());
            pstmt.setDouble(5, auction.getMinIncrement());
            pstmt.setObject(6, auction.getStartTime());
            pstmt.setObject(7, auction.getEndTime());
            pstmt.setObject(8, auction.getEndTime());
            pstmt.setString(9, auction.getStatus().name());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 2. Lấy phiên đấu giá theo ID
    public Auction getAuctionById(int auctionId) {
        String sql = "SELECT a.*, i.name AS product_name, i.image_path, i.description " +
                "FROM Auctions a " +
                "LEFT JOIN Items i ON a.item_id = i.item_id " +
                "WHERE a.auction_id = ?";
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
        String sql = "SELECT a.*, i.name AS product_name, i.image_path, i.description " +
                "FROM Auctions a " +
                "LEFT JOIN Items i ON a.item_id = i.item_id";
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
        String sql = "SELECT a.*, i.name AS product_name, i.image_path, i.description " +
                "FROM Auctions a " +
                "LEFT JOIN Items i ON a.item_id = i.item_id " +
                "WHERE a.status = ?";
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
        String sql = "SELECT a.*, i.name AS product_name, i.image_path, i.description " +
                "FROM Auctions a " +
                "LEFT JOIN Items i ON a.item_id = i.item_id " +
                "WHERE a.seller_id = ?";
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

    // 6. insertBid + updateCurrentPrice trong 1 transaction (Chống Race Condition)
    public boolean placeBidTransaction(BidTransaction bid, double newPrice, int bidderId) {
        String insertBidSql    = "INSERT INTO Bids (auction_id, bidder_id, bid_amount) VALUES (?, ?, ?)";
        String updatePriceSql  = "UPDATE Auctions SET current_price = ?, winner_id = ? " +
                "WHERE auction_id = ? AND status = 'RUNNING' AND current_price < ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps1 = conn.prepareStatement(insertBidSql)) {
                    ps1.setInt(1, bid.getAuctionId());
                    ps1.setInt(2, bid.getBidderId());
                    ps1.setDouble(3, bid.getBidAmount());
                    ps1.executeUpdate();
                }

                try (PreparedStatement ps2 = conn.prepareStatement(updatePriceSql)) {
                    ps2.setDouble(1, newPrice);
                    ps2.setInt(2, bidderId);
                    ps2.setInt(3, bid.getAuctionId());
                    ps2.setDouble(4, newPrice);
                    int rows = ps2.executeUpdate();
                    if (rows == 0) {
                        conn.rollback();
                        System.err.println("Giá vừa bị vượt qua bởi người khác, vui lòng thử lại!");
                        return false;
                    }
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 7. Cập nhật trạng thái phiên đấu giá
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

    // 8. Cập nhật thời gian kết thúc (Anti-sniping)
    public boolean updateEndTime(int auctionId, LocalDateTime newEndTime) {
        String sql = "UPDATE Auctions SET end_time = ? WHERE auction_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, newEndTime);
            pstmt.setInt(2, auctionId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 9. Lấy thời gian kết thúc nguyên bản
    public LocalDateTime getOriginalEndTime(int auctionId) {
        String sql = "SELECT original_end_time FROM Auctions WHERE auction_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, auctionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getObject("original_end_time", LocalDateTime.class);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi lấy original_end_time: " + e.getMessage());
        }
        return null;
    }

    // 10. Lấy các phiên đang diễn ra mà người dùng ĐÃ ĐẶT GIÁ THÀNH CÔNG
    public List<Auction> getAuctionsByBidder(int bidderId) {
        List<Auction> list = new ArrayList<>();
        String sql = "SELECT DISTINCT a.auction_id, a.item_id, a.seller_id, a.start_price, " +
                "a.current_price, a.min_increment, a.start_time, a.end_time, a.status, " +
                "a.winner_id, a.original_end_time, i.name as product_name, i.image_path, i.description " +
                "FROM Auctions a " +
                "JOIN Bids b ON a.auction_id = b.auction_id " +
                "JOIN Items i ON a.item_id = i.item_id " +
                "WHERE b.bidder_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, bidderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToAuction(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi truy vấn getAuctionsByBidder: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    // 11. Cập nhật giá hiện tại trực tiếp
    public boolean updateCurrentPrice(int auctionId, double newPrice, int winnerId) {
        String sql = "UPDATE Auctions SET current_price = ?, winner_id = ? WHERE auction_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, newPrice);
            pstmt.setInt(2, winnerId);
            pstmt.setInt(3, auctionId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 12. Đếm số phiên đấu giá đã thắng
    public int countWonAuctions(int bidderId) {
        String sql = "SELECT COUNT(*) FROM Auctions WHERE winner_id = ? AND status = 'FINISHED'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, bidderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    // 13. Đếm số lượng phiên active bằng COUNT
    public int countActiveAuctionsByUser(int userId) {
        String sql = "SELECT COUNT(DISTINCT auction_id) FROM Bids WHERE bidder_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi đếm số phiên hoạt động trong AuctionDAO: " + e.getMessage());
        }
        return 0;
    }

    // 🛠️ ĐÃ TỐI ƯU TOÀN DIỆN: Hàm map ResultSet sang Object Auction bảo vệ thuộc tính
    private Auction mapResultSetToAuction(ResultSet rs) throws SQLException {
        Auction auction = new Auction();
        auction.setId(rs.getInt("auction_id"));

        // Trả về String tương thích với cấu trúc của Model, đồng bộ với DB kiểu INT
        auction.setItemId(String.valueOf(rs.getInt("item_id")));

        auction.setSellerId(rs.getInt("seller_id"));
        auction.setStartPrice(rs.getDouble("start_price"));
        auction.setCurrentPrice(rs.getDouble("current_price"));
        auction.setMinIncrement(rs.getDouble("min_increment"));

        Timestamp startTs = rs.getTimestamp("start_time");
        if (startTs != null) auction.setStartTime(startTs.toLocalDateTime());

        Timestamp endTs = rs.getTimestamp("end_time");
        if (endTs != null) auction.setEndTime(endTs.toLocalDateTime());

        auction.setStatus(AuctionStatus.valueOf(rs.getString("status")));

        try {
            auction.setOriginalEndTime(rs.getObject("original_end_time", LocalDateTime.class));
        } catch (Exception ignored) {}

        int winnerId = rs.getInt("winner_id");
        if (!rs.wasNull()) {
            auction.setWinnerId(winnerId);
        }

        // 🛠️ ĐÃ SỬA: Map bổ sung thông tin hiển thị giao diện chi tiết để loại bỏ lỗi "Đang tải..."
        try { auction.setProductName(rs.getString("product_name")); } catch (SQLException ignored) {}
        try { auction.setImagePath(rs.getString("image_path")); } catch (SQLException ignored) {}
        try { auction.setDescription(rs.getString("description")); } catch (SQLException ignored) {}

        return auction;
    }

    public List<Auction> getAllAuctionsWithConnection(Connection conn) throws SQLException {
        List<Auction> list = new ArrayList<>();
        String sql = "SELECT a.*, i.name AS product_name, i.image_path, i.description " +
                "FROM Auctions a " +
                "LEFT JOIN Items i ON a.item_id = i.item_id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapResultSetToAuction(rs));
        }
        return list;
    }

    public boolean updateStatusWithConnection(Connection conn, int id, AuctionStatus status) throws SQLException {
        String sql = "UPDATE Auctions SET status = ? WHERE auction_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    public Map<String, Integer> getDashboardStats(int userId) throws SQLException {
        Map<String, Integer> stats = new HashMap<>();
        String sql = "SELECT " +
                "(SELECT COUNT(*) FROM Bids WHERE bidder_id = ?) as total, " +
                "(SELECT COUNT(*) FROM Auctions WHERE winner_id = ?) as won";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int total = rs.getInt("total");
                int won = rs.getInt("won");
                stats.put("total", total);
                stats.put("won", won);
                stats.put("lost", Math.max(0, total - won));
            }
        }
        return stats;
    }

    public Auction getAuctionByItemId(String itemId) throws SQLException {
        String sql = "SELECT a.*, i.name AS product_name, i.image_path, i.description " +
                "FROM Auctions a " +
                "LEFT JOIN Items i ON a.item_id = i.item_id " +
                "WHERE a.item_id = ? AND a.status = 'RUNNING' LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // 🛠️ ĐÃ SỬA: Chuyển sang setInt để tìm kiếm chuẩn xác kiểu dữ liệu DB mới
            ps.setInt(1, Integer.parseInt(itemId));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Auction a = mapResultSetToAuction(rs);
                    System.out.println("DEBUG startTime=" + a.getStartTime() + " endTime=" + a.getEndTime());
                    return a;
                }
            }
        } catch (NumberFormatException e) {
            System.err.println("❌ Lỗi định dạng itemId không phải số nguyên: " + itemId);
        }
        return null;
    }

    public int countFinishedAuctionsByUser(int userId) {
        String sql = "SELECT COUNT(DISTINCT a.auction_id) FROM Auctions a " +
                "JOIN Bids b ON a.auction_id = b.auction_id " +
                "WHERE b.bidder_id = ? AND a.status = 'FINISHED'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }
}