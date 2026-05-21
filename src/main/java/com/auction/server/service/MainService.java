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

    public double getBalance() {
        return 0.0;
    }

    public int getOngoingCount() {
        return 0;
    }

    public int getWonCount() {
        return 0;
    }

    // ✅ ĐÃ SỬA CHUẨN 100%: Lấy danh sách sản phẩm THẬT từ bảng 'Items' viết hoa
    public List<Item> getHotAuctions() {
        List<Item> hotItems = new ArrayList<>();

        // 🟢 Khớp chuẩn tên bảng 'Items' viết hoa đầu (tránh lỗi trên hệ thống Linux/Cloud)
        String query = "SELECT * FROM Items ORDER BY created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                // 1. Đọc chuỗi JSON gốc từ DB (Ví dụ: {"image": "vangogh.jpg"})
                String jsonAttributes = rs.getString("attributes");
                String imageFile = "default.png"; // Mặc định nếu lỗi

                // 2. Bóc tách chuỗi JSON bằng regex đơn giản (để không cần cài thêm thư viện JSON)
                if (jsonAttributes != null && jsonAttributes.contains("\"image\"")) {
                    try {
                        // Cắt chuỗi để lấy chữ "vangogh.jpg" nằm giữa các dấu ngoặc kép
                        int start = jsonAttributes.indexOf("\"image\"") + 7;
                        start = jsonAttributes.indexOf("\"", start) + 1;
                        int end = jsonAttributes.indexOf("\"", start);
                        if (start > 0 && end > start) {
                            imageFile = jsonAttributes.substring(start, end).trim();
                        }
                    } catch (Exception e) {
                        System.err.println("Lỗi phân tích JSON attributes: " + e.getMessage());
                    }
                }

                // 3. Truyền tên file ảnh sạch (ví dụ: "vangogh.jpg") vào đối tượng Electronics
                Electronics item = new Electronics(
                        rs.getString("item_id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getDouble("starting_price"),
                        rs.getInt("owner_id"),
                        rs.getInt("category_id"),
                        rs.getString("status"),
                        imageFile, // ✅ Đã là tên file sạch để Client sử dụng trực tiếp!
                        12
                );
                hotItems.add(item);
            }

            // In log để bạn tự tin theo dõi ở Console của Server
            System.out.println("[Server MainService] thành công từ DB lên " + hotItems.size() + " sản phẩm thật!");

        } catch (SQLException e) {
            System.err.println(" Lỗi truy vấn bảng Items trong MainService! Hãy kiểm tra lại kết nối hoặc chính tả!");
            e.printStackTrace();
        }

        return hotItems;
    }

    public void logout() {
        System.out.println("Đang đăng xuất khỏi hệ thống...");
    }
}