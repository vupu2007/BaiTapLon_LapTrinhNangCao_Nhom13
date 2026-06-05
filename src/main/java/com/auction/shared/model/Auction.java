package com.auction.shared.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.auction.shared.exception.AuctionClosedException;
import com.auction.shared.exception.InvalidBidException;

public class Auction implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    // Các trạng thái phiên đấu giá đồng bộ khớp chuẩn cơ sở dữ liệu
    public enum AuctionStatus {
        OPEN,      // Chờ bắt đầu
        RUNNING,   // Đang diễn ra
        FINISHED,  // Đã kết thúc
        PAID,      // Đã thanh toán
        CANCELED   // Bị hủy
    }

    private int id;                     // ID phiên đấu giá
    private String itemId;              // ID vật phẩm (VARCHAR trong DB)
    private int sellerId;               // ID người tạo phiên đấu giá
    private double startPrice;          // Giá khởi điểm
    private double currentPrice;        // Giá cao nhất hiện tại
    private double minIncrement;        // Bước giá tối thiểu
    private Integer winnerId;           // ID người thắng cuộc
    private LocalDateTime startTime;    // Thời gian bắt đầu
    private LocalDateTime endTime;      // Thời gian kết thúc
    private LocalDateTime originalEndTime; // 🌟 THÊM: Lưu thời gian kết thúc gốc phục vụ Anti-sniping
    private AuctionStatus status;       // Trạng thái phiên đấu giá

    private String productName;
    private String imagePath;
    private String description;

    private int bidCount;
    public int getBidCount() { return bidCount; }
    public void setBidCount(int bidCount) { this.bidCount = bidCount; }

    // Thông tin bổ trợ phục vụ hiển thị trực tiếp trên giao diện Client Card
    // 🔥 THÊM: Sửa lỗi "Cannot resolve method 'setDescription'" cho MainLayoutController

    private Account account;// Thông tin tài khoản người bán đính kèm từ DB

    private String sellerName;
    private String winnerName;

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public String getWinnerName() { return winnerName; }
    public void setWinnerName(String winnerName) { this.winnerName = winnerName; }
    /**
     * 🔥 SỬA KIẾN TRÚC: Thêm từ khóa 'transient'
     * Từ khóa này báo hiệu cho Java biết KHÔNG mã hóa danh sách này khi truyền qua Socket,
     * tránh tuyệt đối lỗi sập luồng mạng (NotSerializableException) khi chạy ứng dụng lớn.
     */
    private transient List<Observer> observers = new ArrayList<>();

    public Auction() {}

    // Constructor đầy đủ tham số (Đã cập nhật originalEndTime)
    public Auction(int id, String itemId, int sellerId, double startPrice, double currentPrice,
                   double minIncrement, Integer winnerId, LocalDateTime startTime, LocalDateTime endTime,
                   LocalDateTime originalEndTime, AuctionStatus status, Account account) {
        this.id = id;
        this.itemId = itemId;
        this.sellerId = sellerId;
        this.startPrice = startPrice;
        this.currentPrice = currentPrice;
        this.minIncrement = minIncrement;
        this.winnerId = winnerId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.originalEndTime = originalEndTime;
        this.status = status;
        this.account = account;
    }

    // ================= GETTER VÀ SETTER CHUẨN HÓA =================

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

    // 🌟 THÊM: Getter cho originalEndTime
    public LocalDateTime getOriginalEndTime() { return originalEndTime; }

    // 🌟 THÊM: Setter cho originalEndTime
    public void setOriginalEndTime(LocalDateTime originalEndTime) { this.originalEndTime = originalEndTime; }

    public AuctionStatus getStatus() { return status; }
    public void setStatus(AuctionStatus status) { this.status = status; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public String getDescription() { return description; } // 🔥 THÊM ĐỂ ĐỒNG BỘ LOGIC LAYOUT
    public void setDescription(String description) { this.description = description; }

    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }

    public List<Observer> getObservers() {
        if (observers == null) { // Đề phòng trường hợp sau khi truyền qua mạng bị gán thành null
            observers = new ArrayList<>();
        }
        return observers;
    }
    public void setObservers(List<Observer> observers) { this.observers = observers; }

    // Kiểm tra phiên đấu giá còn hiệu lực không
    public boolean isActive() {
        return this.status == AuctionStatus.RUNNING && LocalDateTime.now().isBefore(this.endTime);
    }

    /**
     * Đặt giá vào phiên đấu giá.
     * @throws AuctionClosedException nếu phiên không ở trạng thái RUNNING
     * @throws InvalidBidException nếu giá đặt không hợp lệ
     */
    public void placeBid(int bidderId, double amount)
            throws AuctionClosedException, InvalidBidException {

        if (this.status != AuctionStatus.RUNNING) {
            throw new AuctionClosedException(
                    "Phiên đấu giá không còn hoạt động",
                    this.id,
                    this.status
            );
        }

        if (LocalDateTime.now().isAfter(this.endTime)) {
            throw new AuctionClosedException(
                    "Phiên đấu giá đã hết thời gian",
                    this.id,
                    this.status
            );
        }

        double minimumRequired = this.currentPrice + this.minIncrement;
        if (amount < minimumRequired) {
            throw new InvalidBidException(
                    "Giá đặt phải cao hơn giá hiện tại ít nhất " + this.minIncrement,
                    amount,
                    minimumRequired
            );
        }

        this.currentPrice = amount;
        this.winnerId = bidderId;
    }
}


