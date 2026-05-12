package com.auction.shared.model;


/**
 * Lớp trừu tượng Account đại diện cho tài khoản người dùng trong hệ thống đấu giá.
 */
public abstract class Account implements Entity {
    protected String id;
    protected String username;
    protected String password;
    protected String role;
    protected String email;
    // Constructor
    public Account(String id, String username, String password, String email, String role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = role;
    }
    // --- GETTERS ---
    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
    public String getEmail() { return email; }

    // --- SETTERS  ---
    public void setEmail(String email) {
        this.email = email; // Gán giá trị tham số vào thuộc tính của object
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public void setId(String id) {
        this.id = id;
    }
    // --- PHƯƠNG THỨC TRỪU TƯỢNG ---
    /**
     * Trả về chuỗi hiển thị vai trò (ví dụ: "Người mua", "Người bán")
     */
    public abstract String displayRole();

    /**
     * Điều hướng đến Dashboard tương ứng của từng loại tài khoản
     */
    public abstract void navigateDashboard();
}