package com.auction.service;

import com.auction.model.Bidder;
import com.auction.model.Item;

public class AuctionService {
    private String auctionId;
    private Item auctionItem;
    private double currentHighestBid;
    private Bidder highestBidder;
    private boolean isRunning;

    public AuctionService(String auctionId, Item auctionItem) {
        this.auctionId = auctionId;
        this.auctionItem = auctionItem;
        this.currentHighestBid = auctionItem.getStartingPrice();
        this.isRunning = true;
    }

    public synchronized boolean placeBid(Bidder bidder, double bidAmount) {
        if (!isRunning) {
            System.out.println("Phiên đã đóng!");
            return false;
        }

        if (bidAmount <= currentHighestBid) {
            System.out.println("Bid không hợp lệ!");
            return false;
        }

        this.currentHighestBid = bidAmount;
        this.highestBidder = bidder;

        System.out.println("Bid thành công: " + bidder.getUsername());
        return true;
    }

    public void closeAuction() {
        this.isRunning = false;

        System.out.println("Auction kết thúc!");
        if (highestBidder != null) {
            System.out.println("Winner: " + highestBidder.getUsername());
        }
    }

    // getter
    public double getCurrentHighestBid() { return currentHighestBid; }
    public Bidder getHighestBidder() { return highestBidder; }
    public boolean isRunning() { return isRunning; }
}