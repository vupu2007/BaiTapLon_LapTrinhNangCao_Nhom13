package com.auction.dao;

import com.auction.model.Item;
import com.auction.model.Electronics;
import com.auction.util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO {

    // 1. Thêm sản phẩm mới vào DB
    public boolean insertItem(Item item) {
        String sql = "INSERT INTO Items (item_id, name, description, starting_price, category_id, owner_id, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, item.getItemId());
            pstmt.setString(2, item.getName());
            pstmt.setString(3, item.getDescription());
            pstmt.setDouble(4, item.getStartingPrice());
            pstmt.setInt(5, item.getCategoryId());
            pstmt.setInt(6, item.getOwnerId());
            pstmt.setString(7, item.getStatus());

            return pstmt.executeUpdate() > 0;
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
                if (rs.next()) {
                    return mapResultSetToItem(rs);
                }
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

    // 4. Lấy danh sách sản phẩm theo chủ sở hữu (dành cho Seller)
    public List<Item> getItemsByOwner(int ownerId) {
        List<Item> list = new ArrayList<>();
        String sql = "SELECT * FROM Items WHERE owner_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, ownerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToItem(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // 5. Cập nhật trạng thái sản phẩm (AVAILABLE → IN_AUCTION → SOLD)
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

    // 6. Cập nhật thông tin sản phẩm (dành cho Seller chỉnh sửa)
    public boolean updateItem(Item item) {
        String sql = "UPDATE Items SET name = ?, description = ?, starting_price = ?, category_id = ? WHERE item_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, item.getName());
            pstmt.setString(2, item.getDescription());
            pstmt.setDouble(3, item.getStartingPrice());
            pstmt.setInt(4, item.getCategoryId());
            pstmt.setString(5, item.getItemId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 7. Xóa sản phẩm (chỉ xóa được khi status = AVAILABLE)
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

    // --- Helper: map ResultSet sang Item object ---
    private Item mapResultSetToItem(ResultSet rs) throws SQLException {
        Electronics item = new Electronics();
        item.setItemId(rs.getString("item_id"));
        item.setName(rs.getString("name"));
        item.setDescription(rs.getString("description"));
        item.setStartingPrice(rs.getDouble("starting_price"));
        item.setCategoryId(rs.getInt("category_id"));
        item.setOwnerId(rs.getInt("owner_id"));
        item.setStatus(rs.getString("status"));
        return item;
    }
}