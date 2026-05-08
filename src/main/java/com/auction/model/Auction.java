package com.auction.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Auction implements Serializable {
    private static final long serialVersionUID = 1L;

    // Trạng thái của phiên đấu giá
    public enum AuctionStatus {
        PENDING, // Chờ bắt đầu
        ACTIVE,  // Đang diễn ra
        ENDED,   // Đã kết thúc
        CANCELLED // Bị hủy
    }

    private int id;                 // ID phiên đấu giá
    private int itemId;             // ID của sản phẩm được đấu giá
    private int sellerId;           // ID của người bán
    private double startingPrice;   // Giá khởi điểm
    private double currentPrice;    // Giá cao nhất hiện tại
    private Integer highestBidderId;// ID người đang trả giá cao nhất
    private LocalDateTime startTime; // Thời gian bắt đầu
    private LocalDateTime endTime;   // Thời gian kết thúc
    private AuctionStatus status;// Trạng thái phiên đấu giá
    private double stepPrice;
    private User currentWinner;

    public Auction() {
    }

    public Auction(int id, int itemId, int sellerId, double startingPrice, double currentPrice,
                   Integer highestBidderId, LocalDateTime startTime, LocalDateTime endTime, AuctionStatus status, double stepPrice,User currentWinner) {
        this.id = id;
        this.itemId = itemId;
        this.sellerId = sellerId;
        this.startingPrice = startingPrice;
        this.currentPrice = currentPrice;
        this.highestBidderId = highestBidderId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.stepPrice = stepPrice;
        this.currentWinner = null;


    }


    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }

    public int getSellerId() { return sellerId; }
    public void setSellerId(int sellerId) { this.sellerId = sellerId; }

    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }

    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public Integer getHighestBidderId() { return highestBidderId; }
    public void setHighestBidderId(Integer highestBidderId) { this.highestBidderId = highestBidderId; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public User getCurrentWinner(){
        return this.currentWinner;
    }

    public AuctionStatus getStatus() { return status; }
    public void setStatus(AuctionStatus status) { this.status = status; }

    // Kiểm tra xem phiên đấu giá còn hiệu lực không
    public boolean isActive() {
        return this.status == AuctionStatus.ACTIVE && LocalDateTime.now().isBefore(this.endTime);
    }
    public boolean isExpired(){
        return LocalDateTime.now().isAfter(startTime);
    }
    public boolean hasStarted(){
        return LocalDateTime.now().isAfter(endTime);
    }
    public synchronized boolean placeBid(Bidder u ,double amount ){// //giá đặt có lớn hơn hiện tại kooong, người dùng có đủ tiền ko
        if (!hasStarted()){
            System.out.println(" Phiên đấu giá chưa bắt đầu. Hãy quay lại sau! ");
            return false;
        }
        if (isExpired()){
            this.status = AuctionStatus.ENDED;//Tự động cập nhật trạng thái hết giờ
            System.out.println(" Phiên đấu giá đã kết thúc! ");
            return false;
        }
        if(u.getBalance() < amount){
            System.out.println(" Số dư của bạn không đủ! ");
            return false;
        }
        double minRequired;
        if(currentWinner == null){// NullPointerException phải xử lí khi lâấy dữ liệu ra ngoài giao diện
            minRequired = startingPrice;}
        else{
            minRequired = currentPrice + stepPrice;
        }

        if (amount < minRequired) {
            System.out.println(" Giá đặt không hợp lệ! ");
            return false;
        }

        // Cập nhật người thắng mới và giá mới
        this.currentPrice = amount;
        this.currentWinner = u;
        notifyObservers();
        return true;
    }

}
