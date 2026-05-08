package com.auction.model;

// THAY ĐỔI: bỏ "abstract" — User giờ là class cụ thể, vừa mua vừa bán được
public class User extends Account {
    protected double balance;

    // THAY ĐỔI: bỏ tham số "role" khỏi constructor
    // vì role được lấy từ DB, không cần truyền tay nữa
    public User(String id, String username, String password, String email, String role, double balance) {
        super(id, username, password, email, role);
        this.balance = balance;
    }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    // THAY ĐỔI: thêm switchRole() để hỗ trợ chuyển đổi vai trò
    public void switchRole(String newRole) {
        this.role = newRole;
    }

    // THAY ĐỔI: implement 2 method abstract từ Account
    // (trước đây Bidder/Seller implement, giờ User tự implement)
    @Override
    public String displayRole() {
        return role;
    }

    @Override
    public void navigateDashboard() {
        System.out.println("Redirecting to dashboard for role: " + role);
    }
}