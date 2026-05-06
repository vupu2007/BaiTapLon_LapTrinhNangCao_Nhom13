package com.auction.model;


public abstract class User extends Account implements Entity {
    protected String id;
    protected String username;
    protected String password;
    protected String role;// Thêm mới
    protected double balance;

    public User(String id, String username, String password, String role, double initialBalance) {
        super(id, username, password , role );
        this.balance = initialBalance;
    }
    public double getBalance() {
        return balance;
    }

    // Hàm trừ tiền (dùng khi đặt giá hoặc thanh toán)
    public boolean deductBalance(double amount) {
        if (this.balance >= amount) {
            this.balance -= amount;
            return true;
        }
        return false;
    }

    // Hàm cộng tiền (dùng cho Seller khi bán được hàng, hoặc khi User nạp tiền)
    public void addBalance(double amount) {
        this.balance += amount;
    }
}
