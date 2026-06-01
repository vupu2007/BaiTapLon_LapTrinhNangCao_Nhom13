package com.auction.shared.model;

// THAY ĐỔI: đổi thành abstract vì Electronics/Art/Vehicle mới là class cụ thể
public abstract class Item implements Entity, java.io.Serializable {
    private static final long serialVersionUID = 1L;
    protected String imagePath;
    protected String itemId;
    protected String name;
    protected String description;
    protected double startingPrice;
    protected int ownerId;
    protected int categoryId;   // THÊM: khớp với DB
    protected String status;    // THÊM: 'AVAILABLE', 'IN_AUCTION', 'SOLD'

    // 1. Constructor rỗng
    public Item() {}

    // 2. Constructor đầy đủ
    public Item(String itemId, String name, String description, double startingPrice,
                int ownerId, int categoryId, String status, String imagePath) {
        this.itemId = itemId;
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.ownerId = ownerId;
        this.categoryId = categoryId;
        this.status = status;
        this.imagePath = imagePath ;
    }

    @Override
    public String getId() { return itemId; }

    // --- SETTERS ---
    public void setItemId(String itemId) { this.itemId = itemId; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }
    public void setOwnerId(int ownerId) { this.ownerId = ownerId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    public void setStatus(String status) { this.status = status; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    // --- GETTERS ---
    public String getItemId() { return itemId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getStartingPrice() { return startingPrice; }
    public int getOwnerId() { return ownerId; }
    public int getCategoryId() { return categoryId; }
    public String getStatus() { return status; }
    public String getImagePath() { return imagePath; }

    // abstract để buộc class con override
    public abstract void printInfo();

    private int auctionId;
    private double currentPrice;

    public int getAuctionId() { return auctionId; }
    public void setAuctionId(int auctionId) { this.auctionId = auctionId; }
    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    private String endTimeStr;
    public String getEndTimeStr() { return endTimeStr; }
    public void setEndTimeStr(String endTimeStr) { this.endTimeStr = endTimeStr; }

    private String startTimeStr;
    public String getStartTimeStr() { return startTimeStr; }
    public void setStartTimeStr(String startTimeStr) { this.startTimeStr = startTimeStr; }

    private String auctionStatus;
    public String getAuctionStatus() { return auctionStatus; }
    public void setAuctionStatus(String auctionStatus) { this.auctionStatus = auctionStatus; }
}

