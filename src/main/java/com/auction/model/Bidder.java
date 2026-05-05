package com.auction.model;

public class Bidder extends User {
    private double maxBid;
    private double increment;

    // Sửa Constructor: Thêm email và role vào tham số
    public Bidder(String id, String username, String password, String email, String role) {
        // Truyền đủ 5 tham số lên class cha User qua super()
        super(id, username, password, email, role);
        this.maxBid = 0.0;
        this.increment = 0.0;
    }

    @Override
    public String displayRole() {
        return "Vai trò: Bidder";
    }

    // Phú có thể giữ lại hoặc thêm Getter/Setter cho maxBid và increment nếu cần
    public double getMaxBid() { return maxBid; }
    public void setMaxBid(double maxBid) { this.maxBid = maxBid; }
}