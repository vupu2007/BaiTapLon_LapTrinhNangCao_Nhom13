package com.auction.server.dao;

import com.auction.shared.model.Art;
import com.auction.shared.model.Item;
import com.auction.shared.model.Electronics;
import com.auction.server.util.DatabaseConnection;
import com.auction.shared.model.Vehicle;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO {

    // 1. Thêm sản phẩm mới vào DB
    public boolean insertItem(Electronics item) {
        String query = "INSERT INTO Items (item_id, name, description, starting_price, category_id, owner_id, status, attributes, image_path) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, item.getItemId());
            stmt.setString(2, item.getName());
            stmt.setString(3, item.getDescription());
            stmt.setDouble(4, item.getStartingPrice());
            stmt.setInt(5, item.getCategoryId());
            stmt.setInt(6, item.getOwnerId());
            stmt.setString(7, item.getStatus());
            stmt.setString(8, item.getBrand());
            stmt.setString(9, item.getImagePath());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 2. Lấy 1 sản phẩm theo ID
    public Item getItemById(String itemId) {
        String sql = "SELECT * FROM Items WHERE item_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, itemId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return mapResultSetToItem(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 3. Lấy toàn bộ danh sách sản phẩm
    public List<Item> getAllItems() {
        List<Item> list = new ArrayList<>();
        String sql = "SELECT * FROM Items";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapResultSetToItem(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // 4. Lấy danh sách kèm theo logic thời gian đấu giá cho Seller
    public List<Item> getItemsByOwner(int ownerId) {
        List<Item> list = new ArrayList<>();
        String sql = "SELECT i.*, a.start_time, a.end_time " +
                "FROM Items i " +
                "LEFT JOIN Auctions a ON i.item_id = a.item_id " +
                "WHERE i.owner_id = ? " +
                "ORDER BY i.item_id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, ownerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Item item = mapResultSetToItem(rs);
                    Timestamp start = rs.getTimestamp("start_time");
                    Timestamp end = rs.getTimestamp("end_time");

                    if (item instanceof Electronics && start != null && end != null) {
                        ((Electronics) item).setBrand(start.toString() + "|" + end.toString());
                    } else if (item instanceof Electronics) {
                        ((Electronics) item).setBrand("");
                    }
                    list.add(item);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // 5. Cập nhật trạng thái sản phẩm
    public boolean updateStatus(String itemId, String newStatus) {
        String sql = "UPDATE Items SET status = ? WHERE item_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newStatus);
            pstmt.setString(2, itemId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 6. Cập nhật thông tin sản phẩm
    public boolean updateItem(Item item) {
        String sql = "UPDATE Items SET name = ?, description = ?, starting_price = ?, category_id = ?, image_path = ? WHERE item_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, item.getName());
            pstmt.setString(2, item.getDescription());
            pstmt.setDouble(3, item.getStartingPrice());
            pstmt.setInt(4, item.getCategoryId());
            pstmt.setString(5, item.getImagePath());
            pstmt.setString(6, item.getItemId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 7. Xóa sản phẩm
    public boolean deleteItem(String itemId) {
        String sql = "DELETE FROM Items WHERE item_id = ? AND status = 'AVAILABLE'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, itemId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 🛠️ 8. BỔ SUNG HÀM THIẾU CHÍ MẠNG: Lấy danh sách sản phẩm đang Hot Auctions
    public List<Item> getHotAuctions() {
        List<Item> list = new ArrayList<>();
        String sql = "SELECT i.* FROM Items i " +
                "JOIN Auctions a ON i.item_id = a.item_id " +
                "WHERE a.status = 'RUNNING' " +
                "LIMIT 10";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToItem(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi getHotAuctions: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    // --- Helper: map ResultSet sang Item object ---
    private Item mapResultSetToItem(ResultSet rs) throws SQLException {
        Item item;
        int categoryId = rs.getInt("category_id");

        // Tùy theo logic phân loại category của database nhóm bạn:
        if (categoryId == 1) {
            item = new Electronics();
        } else if (categoryId == 2) {
            item = new Art(); // Ví dụ
        } else {
            item = new Vehicle(); // Hoặc một class mặc định nào đó
        }

        item.setItemId(rs.getString("item_id"));
        item.setName(rs.getString("name"));
        item.setDescription(rs.getString("description"));
        item.setStartingPrice(rs.getDouble("starting_price"));
        item.setCategoryId(categoryId);
        item.setOwnerId(rs.getInt("owner_id"));
        item.setStatus(rs.getString("status"));
        item.setImagePath(rs.getString("image_path"));

        // Nếu các class con có thuộc tính riêng, bạn cần ép kiểu ở đây
        // Ví dụ: if (item instanceof Electronics) ((Electronics)item).setBrand(rs.getString("attributes"));

        return item;
    }

    public boolean startAuction(String itemId, int sellerId, double startPrice, String startTime, String endTime) {
        String sql = "INSERT INTO Auctions (item_id, seller_id, start_price, current_price, start_time, end_time, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, 'RUNNING')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, itemId);
            pstmt.setInt(2, sellerId);
            pstmt.setDouble(3, startPrice);
            pstmt.setDouble(4, startPrice);
            pstmt.setString(5, startTime);
            pstmt.setString(6, endTime);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}