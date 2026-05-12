package com.auction.exception;

import com.auction.model.Auction.AuctionStatus;

// Khi người dùng cố đặt giá vào phiên đấu giá không còn ở trạng thái RUNNING (đã đóng, đã kết thúc, bị hủy...)
public class AuctionClosedException extends Exception {

    private final int auctionId;
    private final AuctionStatus currentStatus;

    public AuctionClosedException(String message) {
        super(message);
        this.auctionId = -1;
        this.currentStatus = null;
    }

    public AuctionClosedException(String message, int auctionId, AuctionStatus currentStatus) {
        super(message);
        this.auctionId = auctionId;
        this.currentStatus = currentStatus;
    }

    public int getAuctionId() { return auctionId; }
    public AuctionStatus getCurrentStatus() { return currentStatus; }
}
