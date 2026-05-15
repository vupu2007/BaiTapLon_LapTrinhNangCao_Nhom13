package com.auction;

import com.auction.dao.AuctionDAO;
import com.auction.model.Auction;
import com.auction.service.AuctionScheduler;

import java.time.LocalDateTime;

public class TestScheduler {
    public static void main(String[] args) throws InterruptedException {

        AuctionDAO auctionDAO = new AuctionDAO();

        // Insert thẳng bằng SQL — bỏ qua Foreign Key item_id
        String sql = "INSERT INTO Auctions (seller_id, start_price, current_price, min_increment, start_time, end_time, status) " +
                "VALUES (1, 100000, 100000, 10000, ?, ?, 'RUNNING')";

        try (var conn = com.auction.util.DatabaseConnection.getConnection();
             var pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, java.time.LocalDateTime.now().minusMinutes(10));
            pstmt.setObject(2, java.time.LocalDateTime.now().minusMinutes(1));

            boolean created = pstmt.executeUpdate() > 0;
            System.out.println("Tạo phiên test: " + (created ? "THÀNH CÔNG" : "THẤT BẠI"));

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Khởi động scheduler với interval 5 giây
        AuctionScheduler.getInstance().start();

        // Chờ 10 giây xem scheduler có đóng phiên không
        System.out.println("Chờ scheduler chạy...");
        Thread.sleep(40000);

        AuctionScheduler.getInstance().stop();
        System.out.println("Test xong!");
    }
}