package com.auction.shared.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Auction implements Serializable  {
    private static final long serialVersionUID = 1L;
    private String productName;
    private String imagePath;
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    // THAY ĐỔI: cập nhật enum khớp với DB
    public enum AuctionStatus {
        OPEN,      // Chờ bắt đầu
        RUNNING,   // Đang diễn ra
        FINISHED,  // Đã kết thúc
        PAID,      // Đã thanh toán
        CANCELED   // Bị hủy
    }

    private int id;                     // ID phiên đấu giá
    private String itemId;              // THAY ĐỔI: int → String (item_id là VARCHAR trong DB)
    private int sellerId;               // ID người tạo phiên đấu giá
    private double startPrice;          // Giá khởi điểm
    private double currentPrice;        // Giá cao nhất hiện tại
    private double minIncrement;        // THÊM: bước giá tối thiểu
    private Integer winnerId;           // THAY ĐỔI: highestBidderId → winnerId khớp DB
    private LocalDateTime startTime;    // Thời gian bắt đầu
    private LocalDateTime endTime;      // Thời gian kết thúc
    private AuctionStatus status;       // Trạng thái phiên đấu giá
    private Account account;            // Thông tin tài khoản người bán đính kèm từ DB
    private List<Observer> observers = new ArrayList<>();// danh sách ng nhân đc thông báo


    public Auction() {}

    public Auction(int id, String itemId, int sellerId, double startPrice, double currentPrice,
                   double minIncrement, Integer winnerId,
                   LocalDateTime startTime, LocalDateTime endTime, AuctionStatus status, Account account) {
        this.id = id;
        this.itemId = itemId;
        this.sellerId = sellerId;
        this.startPrice = startPrice;
        this.currentPrice = currentPrice;
        this.minIncrement = minIncrement;
        this.winnerId = winnerId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.account = account;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public int getSellerId() { return sellerId; }
    public void setSellerId(int sellerId) { this.sellerId = sellerId; }

    public double getStartPrice() { return startPrice; }
    public void setStartPrice(double startPrice) { this.startPrice = startPrice; }

    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public double getMinIncrement() { return minIncrement; }
    public void setMinIncrement(double minIncrement) { this.minIncrement = minIncrement; }

    public Integer getWinnerId() { return winnerId; }
    public void setWinnerId(Integer winnerId) { this.winnerId = winnerId; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public AuctionStatus getStatus() { return status; }
    public void setStatus(AuctionStatus status) { this.status = status; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    // 🔥 THÊM MỚI: Hàm Getter và Setter cho thuộc tính Account để lấy tên người bán ("mhuyen") bên Controller
    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }

    // 🔥 THÊM MỚI: Hàm Getter và Setter cho danh sách bộ quan sát Observer
    public List<Observer> getObservers() { return observers; }
    public void setObservers(List<Observer> observers) { this.observers = observers; }

    // Kiểm tra phiên đấu giá còn hiệu lực không
    public boolean isActive() {
        return this.status == AuctionStatus.RUNNING && LocalDateTime.now().isBefore(this.endTime);
    }
}