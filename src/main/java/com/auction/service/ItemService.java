package com.auction.service;

import com.auction.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ItemService {

    public ItemService() {
    }

    /**
     * Thêm sản phẩm mới vào bảng Items
     */
    public boolean addItem(String name, String description, int categoryId, int ownerId) {
        // Lệnh SQL bám sát 100% cấu trúc bảng trong ảnh
        String sql = "INSERT INTO Items (name, description, category_id, owner_id) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Set dữ liệu đúng thứ tự các dấu ?
            pstmt.setString(1, name);
            pstmt.setString(2, description);
            pstmt.setInt(3, categoryId); // Truyền ID của danh mục (ví dụ: Điện thoại, Xe cộ...)
            pstmt.setInt(4, ownerId);    // Truyền ID của người dùng đang đăng nhập

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Đã thêm thành công sản phẩm: " + name);
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Lỗi khi thêm sản phẩm: " + e.getMessage());
        }

        return false;
    }
}