package com.auction.shared.model;
import java.io.Serializable;

/**
 * Lớp trừu tượng Account đại diện cho tài khoản người dùng trong hệ thống đấu giá
 */
public abstract class Account implements Entity,Serializable {
    private static final long serialVersionUID = 1L;
    protected String id;
    protected String username;
    protected String password;
    protected String role;
    protected String email;
    private boolean isLocked;
    // BỔ SUNG: Thuộc tính số dư ví tiền dùng chung cho các tài khoản kế thừa
    protected Double balance;

    // Constructor (Cập nhật lại để gán mặc định balance = 0.0 khi khởi tạo)
    public Account(String id, String username, String password, String email, String role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = role;
        this.balance = 0.0; // Mặc định ví bằng 0 khi tạo tài khoản
    }

    // --- GETTERS ---
    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
    public String getEmail() { return email; }

    // BỔ SUNG: Sửa lỗi "cannot find symbol getBalance()"
    public Double getBalance() { return balance; }

    // --- SETTERS  ---
    // BỔ SUNG: Sửa lỗi cập nhật số dư khi nạp/rút tiền
    public void setBalance(Double balance) { this.balance = balance; }

    public void setEmail(String email) {
        this.email = email;
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

    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean isLocked) {
        this.isLocked = isLocked;
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

    private int isLocked;
    public int getIsLocked() { return isLocked; }
    public void setIsLocked(int isLocked) { this.isLocked = isLocked; }
}