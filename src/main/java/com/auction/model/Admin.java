package com.auction.model;

public class Admin extends User {

     public Admin(String id, String username, String password, String email, String role) {
         super(id, username, password, email, role);
    }

    @Override
    public String displayRole() {
        return "Admin";
    }

    public void manageSystem() {
        System.out.println("Admin [" + this.username + "] đang truy cập hệ thống quản trị...");
    }
}