package com.auction.model;

public class Seller extends User {
    protected double balance;

    public Seller(String id, String username, String password , String role, double initialBalance) {
        super(id, username, password, role,initialBalance);
    }

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