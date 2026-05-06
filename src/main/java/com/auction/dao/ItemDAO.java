package com.auction.dao;

import com.auction.model.Item;
import com.auction.model.Electronics;
import com.auction.util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO {

    // 1. Hàm cập nhật giá (Dùng cho nghiệp vụ đấu giá sau này)
    public boolean updateCurrentPrice(String itemId, double newPrice) {
        String sql = "UPDATE Items SET current_price = ? WHERE item_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, newPrice);
            pstmt.setString(2, itemId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 2. Hàm lấy 1 món hàng theo ID (Để kiểm tra giá trước khi đấu giá)
    public Item getItemById(String itemId) {
        String sql = "SELECT * FROM Items WHERE item_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, itemId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // Tạm thời khởi tạo Electronics nếu bạn muốn dùng kế thừa
                    // Sau này có thể thêm cột 'type' trong DB để phân loại chuẩn hơn
                    Electronics item = new Electronics();
                    item.setItemId(rs.getString("item_id"));
                    item.setName(rs.getString("name"));
                    item.setDescription(rs.getString("description"));
                    item.setStartingPrice(rs.getDouble("starting_price"));
                    item.setCurrentPrice(rs.getDouble("current_price"));
                    item.setOwnerId(rs.getInt("owner_id"));
                    item.setEndTime(rs.getString("end_time"));
                    return item;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 3. Hàm lấy toàn bộ danh sách (Để hiển thị lên giao diện)
    public List<Item> getAllItems() {
        List<Item> list = new ArrayList<>();
        String sql = "SELECT * FROM Items";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Electronics item = new Electronics();
                item.setItemId(rs.getString("item_id"));
                item.setName(rs.getString("name"));
                item.setCurrentPrice(rs.getDouble("current_price"));
                // ... nạp thêm các trường khác nếu cần hiển thị
                list.add(item);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}