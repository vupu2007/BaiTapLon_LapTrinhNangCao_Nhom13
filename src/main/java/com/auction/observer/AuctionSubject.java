package com.auction.observer;

public interface AuctionSubject {
    void addObserver(AuctionObserver observer);
    void removeObserver(AuctionObserver observer);
    void notifyObservers(int auctionId, double newPrice);
}