package com.auction.controller;

import com.auction.model.Bidder;
import com.auction.model.Item;

public class AuctionController {
    private String auctionId;
    private Item auctionItem;
    private double currentHighestBid;
    private Bidder highestBidder;
    private boolean isRunning;

    public AuctionController(String auctionId, Item auctionItem) {
        this.auctionId = auctionId;
        this.auctionItem = auctionItem;
        this.currentHighestBid = auctionItem.getStartingPrice(); // Khởi tạo bằng giá khởi điểm
        this.isRunning = true;
    }

    /**
     * Xử lý logic khi một Bidder đặt giá (Yêu cầu 3.1.3)
     */
    public synchronized boolean placeBid(Bidder bidder, double bidAmount) {
        // Kiểm tra xem phiên đấu giá còn mở không (Yêu cầu 3.1.5)
        if (!isRunning) {
            System.out.println("Lỗi: Phiên đấu giá này đã đóng! [cite: 59]");
            return false;
        }

        // Kiểm tra tính hợp lệ của giá đấu: Phải cao hơn giá hiện tại (Yêu cầu 3.1.3 & 3.1.5)
        if (bidAmount <= currentHighestBid) {
            System.out.println("Lỗi: Giá đặt (" + bidAmount + ") phải cao hơn giá hiện tại (" + currentHighestBid + ")! [cite: 58]");
            return false;
        }

        // Cập nhật người dẫn đầu và mức giá mới (Yêu cầu 3.1.3)
        this.currentHighestBid = bidAmount;
        this.highestBidder = bidder;

        System.out.println("Thành công: " + bidder.getUsername() + " đã vươn lên dẫn đầu với mức giá " + bidAmount);

        // TODO: Ghi lại lịch sử giao dịch vào lớp BidTransaction (Gợi ý 3.3.1)

        return true;
    }

    /**
     * Đóng phiên đấu giá (Yêu cầu 3.1.4)
     */
    public void closeAuction() {
        this.isRunning = false;
        System.out.println("Phiên đấu giá " + auctionId + " đã kết thúc! [cite: 53]");
        if (highestBidder != null) {
            System.out.println("Người thắng cuộc là: " + highestBidder.getUsername() + " với giá " + currentHighestBid + " [cite: 54]");
        } else {
            System.out.println("Không có ai tham gia trả giá.");
        }
    }

    // Các getter cần thiết
    public double getCurrentHighestBid() { return currentHighestBid; }
    public Bidder getHighestBidder() { return highestBidder; }
    public boolean isRunning() { return isRunning; }
}