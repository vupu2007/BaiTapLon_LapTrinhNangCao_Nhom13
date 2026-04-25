package com.auction.controller;

import com.auction.model.Bidder;
import com.auction.service.AuctionService;

public class AuctionController {

    private AuctionService auctionService;

    public AuctionController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    public boolean handlePlaceBid(Bidder bidder, double amount) {
        return auctionService.placeBid(bidder, amount);
    }

    public void handleCloseAuction() {
        auctionService.closeAuction();
    }
}