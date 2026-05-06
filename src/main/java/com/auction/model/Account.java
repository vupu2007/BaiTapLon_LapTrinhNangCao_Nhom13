package com.auction.model;

public abstract class Account implements Entity {
    protected String id;
    protected String username;
    protected String password;
    protected String role;
    protected String email; // Thêm email

    public Account(String id, String username, String password, String email, String role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = role;
    }

    // Getter & Setter
    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
    public String getEmail() { return email; }

    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }

    // Phương thức trừu tượng
    public abstract String displayRole();

    // Phương thức để mỗi lớp con tự quyết định mở giao diện nào
    public abstract void navigateDashboard();
}