package com.auction.model;

public class Bidder extends User {

    public Bidder(String id, String username, String password, String email, double balance) {
        super(id, username, password, email, "BIDDER", balance);
    }

    @Override
    public String displayRole() {
        return "Bidder (Buyer)";
    }

    @Override
    public void navigateDashboard() {
        System.out.println("Redirecting to Bidder Dashboard (Browse Items, Place Bids)...");
        // Gọi hàm load BidderView.fxml tại đây
    }

    public void placeBid() {
        // Logic riêng cho việc đặt giá
    }

}