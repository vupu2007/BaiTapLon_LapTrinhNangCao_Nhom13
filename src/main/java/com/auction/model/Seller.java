package com.auction.model;

public class Seller extends User {

    public Seller(String id, String username, String password, String email, String role) {
        super(id, username, password, email, role);}

    @Override
    public String displayRole() {
        return "Seller";
    }
    public Item createItem(String type) {
        System.out.println("Seller " + username + " is creating a " + type);
        return null;
    }
    public void manageItem(Item item) {
        System.out.println("Seller " + username + " is managing item: " + item);
    }
}