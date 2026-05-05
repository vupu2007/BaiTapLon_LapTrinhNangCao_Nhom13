package com.auction.model;

public abstract class User implements Entity {
    protected String id;
    protected String username;
    protected String password;
    protected String email; // Thêm mới
    protected String role;  // Thêm mới

    public User(String id, String username, String password, String email, String role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = role;
    }

    // Getter và Setter cho các trường mới
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getId() { return id; }

    public abstract String displayRole();
}