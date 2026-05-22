package com.auction.server.service;

import com.auction.server.util.DatabaseConnection;
import com.auction.shared.model.Item;
import com.auction.shared.model.Electronics;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MainService {

    public double getBalance() { return 0.0; }
    public int getOngoingCount() { return 0; }
    public int getWonCount() { return 0; }

    public List<Item> getHotAuctions() {
        List<Item> hotItems = new ArrayList<>();
        String query = "SELECT * FROM Items ORDER BY created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String imageFile = rs.getString("image_path"); // ✅ đọc thẳng image_path

                Electronics item = new Electronics(
                        rs.getString("item_id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getDouble("starting_price"),
                        rs.getInt("owner_id"),
                        rs.getInt("category_id"),
                        rs.getString("status"),
                        imageFile,
                        12
                );
                hotItems.add(item);
            }

            System.out.println("[Server MainService] thành công từ DB lên " + hotItems.size() + " sản phẩm thật!");

        } catch (SQLException e) {
            System.err.println("Lỗi truy vấn bảng Items!");
            e.printStackTrace();
        }

        return hotItems;
    }

    public void logout() {
        System.out.println("Đang đăng xuất khỏi hệ thống...");
    }
}