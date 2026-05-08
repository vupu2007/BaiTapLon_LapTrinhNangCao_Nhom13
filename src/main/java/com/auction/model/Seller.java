package com.auction.model;

public class Seller extends User {

    public Seller(String id, String username, String password, String email, double balance) {
        super(id, username, password, email, "SELLER", balance);
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