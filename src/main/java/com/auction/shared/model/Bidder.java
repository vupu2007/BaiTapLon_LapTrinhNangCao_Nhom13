package com.auction.shared.model;

public class Bidder extends User  {

    // 1. [BỔ SUNG]: Khai báo thêm 2 biến để lưu trữ dữ liệu từ MySQL xuống đối tượng RAM
    private double totalDeposit;
    private double totalWithdraw;

    // 2. Cập nhật Constructor
    public Bidder(String id, String username, String password, String email, double balance) {
        super(id, username, password, email, "BIDDER", balance);
        this.totalDeposit = 0.0;
        this.totalWithdraw = 0.0;
    }

    // 3. [BỔ SUNG]: Định nghĩa các hàm Getter và Setter để xóa sạch lỗi đỏ ở CurrentAccount
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
        return "Bidder (Buyer)";
    }

    @Override
    public void navigateDashboard() {
        System.out.println("Redirecting to Bidder Dashboard (Browse Items, Place Bids)...");
        // Gọi hàm load BidderView.fxml tại đây
    }

    public void placeBid() {
        // Logic riêng cho việc đặt giá
    }
}