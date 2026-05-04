package com.auction.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Auction implements Serializable {
    private static final long serialVersionUID = 1L;


    public enum AuctionStatus {
        PENDING,
        ACTIVE,
        ENDED,
        CANCELLED
    }

    private int id;
    private int itemId;
    private int sellerId;
    private double startingPrice;
    private double currentPrice;
    private Integer highestBidderId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;


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

    public boolean isActive() {
        return this.status == AuctionStatus.ACTIVE && LocalDateTime.now().isBefore(this.endTime);
    }
}