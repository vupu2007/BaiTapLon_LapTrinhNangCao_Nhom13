package com.auction.shared.model;

public class Seller extends User {

    // 1. [BỔ SUNG]: 2 biến lưu trữ tổng nạp và tổng chi
    private double totalDeposit;
    private double totalWithdraw;

    // 2. Cập nhật Constructor để nhận và truyền dữ liệu chuẩn
    public Seller(String id, String username, String password, String email, double balance) {
        super(id, username, password, email, "SELLER", balance);
        this.totalDeposit = 0.0;
        this.totalWithdraw = 0.0;
    }

    // 3. [BỔ SUNG]: Các hàm Getter và Setter cho hai biến mới
    public double getTotalDeposit() {
        return totalDeposit;
    }

    public void setTotalDeposit(double totalDeposit) {
        this.totalDeposit = totalDeposit;
    }

    public double getTotalWithdraw() {
        return totalWithdraw;
    }

    public void setTotalWithdraw(double totalWithdraw) {
        this.totalWithdraw = totalWithdraw;
    }

    @Override
    public String displayRole() {
        return "Seller (Merchant)";
    }

    @Override
    public void navigateDashboard() {
        System.out.println("Redirecting to Seller Dashboard (Post Items, Manage Sales)...");
        // Gọi hàm load SellerView.fxml tại đây
    }

    public void postItem() {
        // Logic riêng cho việc đăng sản phẩm mới
    }
}