package com.auction.shared.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class BidTransaction implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;               // ID của giao dịch
    private int auctionId;        // Thuộc phiên đấu giá nào
    private int bidderId;         // Ai là người đặt giá
    private double bidAmount;     // Số tiền đặt
    private LocalDateTime bidTime;// Thời gian đặt giá chính xác
    // Username của người đặt giá — dùng khi load lịch sử từ DB
    private String bidderUsername;

    public String getBidderUsername() { return bidderUsername; }
    public void setBidderUsername(String bidderUsername) { this.bidderUsername = bidderUsername; }

    public BidTransaction() {
    }

    public BidTransaction(int id, int auctionId, int bidderId, double bidAmount, LocalDateTime bidTime) {
        this.id = id;
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getAuctionId() { return auctionId; }
    public void setAuctionId(int auctionId) { this.auctionId = auctionId; }

    public int getBidderId() { return bidderId; }
    public void setBidderId(int bidderId) { this.bidderId = bidderId; }

    public double getBidAmount() { return bidAmount; }
    public void setBidAmount(double bidAmount) { this.bidAmount = bidAmount; }

    public LocalDateTime getBidTime() { return bidTime; }
    public void setBidTime(LocalDateTime bidTime) { this.bidTime = bidTime; }

    @Override
    public String toString() {
        return "BidTransaction{" +
                "id=" + id +
                ", auctionId=" + auctionId +
                ", bidderId=" + bidderId +
                ", bidAmount=" + bidAmount +
                ", bidTime=" + bidTime +
                '}';
    }
}