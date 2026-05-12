package com.auction.shared.model;

public class Admin extends Account {

    public Admin(String id, String username, String password, String email) {
        super(id, username, password, email, "ADMIN");
    }

    @Override
    public String displayRole() {
        return "System Administrator";
    }

    @Override
    public void navigateDashboard() {
        System.out.println("Redirecting to Admin Dashboard (Manage Users, Items, Auctions)...");

    }
}