package com.auction.model;

public class Bidder extends User{
    private double maxBid;
    private double increment;
    public Bidder(String id, String username, String password) {
        super(id, username, password);
        this.maxBid = 0.0;
        this.increment = 0.0;
    }
    public String displayRole(){
        return "Vai trò : Bidder" ;
    }
//continue
}
