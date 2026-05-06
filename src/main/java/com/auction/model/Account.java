package com.auction.model;

public abstract class Account {
    protected int accountId;
    protected String username;
    protected String password;
    protected String email;
     protected String role;

    public Account(int accountId, String username, String password, String email, String fullName, String role) {
        this.accountId = accountId;
        this.username = username;
        this.password = password;
        this.email = email;
         this.role = role;
    }

    // Các Getter cần thiết
    public int getAccountId() { return accountId; }
    public String getUsername() { return username; }
    public String getRole() { return role; }

    // Phương thức trừu tượng: Mỗi loại tài khoản sẽ mở ra giao diện JavaFX khác nhau.
    public abstract void loginSuccessAction();
}