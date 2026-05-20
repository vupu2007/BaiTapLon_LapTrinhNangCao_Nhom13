package com.auction;

import com.auction.server.dao.AutoBidDAO;
import com.auction.server.service.AuctionService;

public class TestAutoBid {
    public static void main(String[] args) {

        AutoBidDAO autoBidDAO = new AutoBidDAO();
        AuctionService auctionService = new AuctionService();

        int auctionId = 3; // thay bằng auction_id thật trong DB
        int bidderId  = 2; // thay bằng account_id thật trong DB
        double maxBid = 500000;

        // Test đăng ký auto-bid
        System.out.println("=== TEST ĐĂNG KÝ AUTO-BID ===");
        boolean registered = auctionService.registerAutoBid(auctionId, bidderId, maxBid);
        System.out.println("Kết quả: " + (registered ? "THÀNH CÔNG" : "THẤT BẠI"));

        // Test kiểm tra đã có auto-bid chưa
        System.out.println("\n=== TEST KIỂM TRA AUTO-BID ===");
        boolean hasAutoBid = autoBidDAO.hasAutoBid(auctionId, bidderId);
        System.out.println("Có auto-bid: " + (hasAutoBid ? "CÓ" : "KHÔNG"));

        // Test lấy maxBid
        System.out.println("\n=== TEST LẤY MAX BID ===");
        double max = autoBidDAO.getMaxBid(auctionId, bidderId);
        System.out.println("MaxBid: " + max);

        // Test cập nhật maxBid
        System.out.println("\n=== TEST CẬP NHẬT MAX BID ===");
        boolean updated = autoBidDAO.updateMaxBid(auctionId, bidderId, 600000);
        System.out.println("Cập nhật: " + (updated ? "THÀNH CÔNG" : "THẤT BẠI"));
    }
}