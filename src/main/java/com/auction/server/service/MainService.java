package com.auction.server.service;

import com.auction.server.util.DatabaseConnection;
import com.auction.shared.model.Auction;
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

    public List<Auction> getHotAuctions() {
        List<Auction> list = new ArrayList<>();
        String query = "SELECT i.*, a.auction_id, a.start_time, a.end_time, a.current_price, a.seller_id, a.winner_id, a.status as auction_status " +
                "FROM Items i JOIN Auctions a ON i.item_id = a.item_id " +
                "WHERE a.status = 'RUNNING' ORDER BY a.start_time DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Auction auction = new Auction();
                auction.setId(rs.getInt("auction_id"));
                auction.setItemId(rs.getString("item_id"));
                auction.setSellerId(rs.getInt("seller_id"));
                auction.setStartPrice(rs.getDouble("starting_price"));
                auction.setCurrentPrice(rs.getDouble("current_price"));
                auction.setStartTime(rs.getObject("start_time", java.time.LocalDateTime.class));
                auction.setEndTime(rs.getObject("end_time", java.time.LocalDateTime.class));
                auction.setStatus(Auction.AuctionStatus.valueOf(rs.getString("auction_status")));
                auction.setProductName(rs.getString("name"));
                auction.setImagePath(rs.getString("image_path"));
                list.add(auction);
            }

            System.out.println("[Server MainService] thành công từ DB lên " + list.size() + " sản phẩm thật!");

        } catch (SQLException e) {
            System.err.println("Lỗi truy vấn!");
            e.printStackTrace();
        }
        return list;
    }

    public void logout() {
        System.out.println("Đang đăng xuất khỏi hệ thống...");
    }
}