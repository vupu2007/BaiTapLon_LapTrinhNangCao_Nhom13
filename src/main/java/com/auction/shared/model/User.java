package com.auction.shared.model;

public abstract class User extends Account {
    protected double balance;

    public User(String id, String username, String password, String email, String role, double balance) {
        super(id, username, password, email, role);
        this.balance = balance;
    }

    public Double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
}