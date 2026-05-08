package com.auction.observer;

public interface AuctionObserver {
    void onBidPlaced(int auctionId, double newPrice);
}