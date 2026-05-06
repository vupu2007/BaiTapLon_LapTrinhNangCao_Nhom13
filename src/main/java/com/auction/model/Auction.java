package com.auction.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Auction implements Serializable {
    private static final long serialVersionUID = 1L;

    // Trạng thái của phiên đấu giá
    public enum AuctionStatus {
        PENDING, // Chờ bắt đầu
        ACTIVE,  // Đang diễn ra
        ENDED,   // Đã kết thúc
        CANCELLED // Bị hủy
    }

    private int id;                 // ID phiên đấu giá
    private int itemId;             // ID của sản phẩm được đấu giá
    private int sellerId;           // ID của người bán
    private double startingPrice;   // Giá khởi điểm
    private double currentPrice;    // Giá cao nhất hiện tại
    private Integer highestBidderId;// ID người đang trả giá cao nhất
    private LocalDateTime startTime; // Thời gian bắt đầu
    private LocalDateTime endTime;   // Thời gian kết thúc
    private AuctionStatus status;    // Trạng thái phiên đấu giá

    public Auction() {
    }

    public Auction(int id, int itemId, int sellerId, double startingPrice, double currentPrice,
                   Integer highestBidderId, LocalDateTime startTime, LocalDateTime endTime, AuctionStatus status) {
        this.id = id;
        this.itemId = itemId;
        this.sellerId = sellerId;
        this.startingPrice = startingPrice;
        this.currentPrice = currentPrice;
        this.highestBidderId = highestBidderId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }

    public int getSellerId() { return sellerId; }
    public void setSellerId(int sellerId) { this.sellerId = sellerId; }

    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }

    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public Integer getHighestBidderId() { return highestBidderId; }
    public void setHighestBidderId(Integer highestBidderId) { this.highestBidderId = highestBidderId; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public AuctionStatus getStatus() { return status; }
    public void setStatus(AuctionStatus status) { this.status = status; }

    // Kiểm tra xem phiên đấu giá còn hiệu lực không
    public boolean isActive() {
        return this.status == AuctionStatus.ACTIVE && LocalDateTime.now().isBefore(this.endTime);
    }
}