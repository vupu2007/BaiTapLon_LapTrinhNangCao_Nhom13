package com.auction;

import com.auction.server.dao.AuctionDAO;
import com.auction.server.util.DatabaseConnection;
import com.auction.server.service.AuctionScheduler;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;

public class TestScheduler {
    public static void main(String[] args) throws InterruptedException {

        AuctionDAO auctionDAO = new AuctionDAO();

        // Insert thẳng bằng SQL — bỏ qua Foreign Key item_id
        String sql = "INSERT INTO Auctions (seller_id, start_price, current_price, min_increment, start_time, end_time, status) " +
                "VALUES (1, 100000, 100000, 10000, ?, ?, 'RUNNING')";

        // Sử dụng Connection và PreparedStatement tường minh
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, LocalDateTime.now().minusMinutes(10));
            pstmt.setObject(2, LocalDateTime.now().minusMinutes(1));

            boolean created = pstmt.executeUpdate() > 0;
            System.out.println("Tạo phiên test: " + (created ? "THÀNH CÔNG" : "THẤT BẠI"));

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Khởi động scheduler với interval 30 giây (đã cấu hình trong AuctionScheduler)
        AuctionScheduler.getInstance().start();

        // Chờ 40 giây xem scheduler có quét và đóng phiên vừa tạo không
        System.out.println("Chờ scheduler chạy...");
        Thread.sleep(40000);

        AuctionScheduler.getInstance().stop();
        System.out.println("Test xong!");
    }
}