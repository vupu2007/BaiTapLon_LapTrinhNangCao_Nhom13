package com.auction.model;

// THAY ĐỔI: đổi thành abstract vì Electronics/Art/Vehicle mới là class cụ thể
public abstract class Item implements Entity {
    protected String itemId;
    protected String name;
    protected String description;
    protected double startingPrice;
    protected double ownerId;
    protected int categoryId;   // THÊM: khớp với DB
    protected String status;    // THÊM: 'AVAILABLE', 'IN_AUCTION', 'SOLD'

    // BỎ: currentPrice và endTime (thuộc về Auction, không phải Item)

    // 1. Constructor rỗng
    public Item() {}

    // 2. Constructor đầy đủ
    public Item(String itemId, String name, String description, double startingPrice,
                int ownerId, int categoryId, String status) {
        this.itemId = itemId;
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.ownerId = ownerId;
        this.categoryId = categoryId;
        this.status = status;
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

    // --- GETTERS ---
    public String getItemId() { return itemId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getStartingPrice() { return startingPrice; }
    public int getOwnerId() { return ownerId; }
    public int getCategoryId() { return categoryId; }
    public String getStatus() { return status; }

    // abstract để buộc class con override
    public abstract void printInfo();
}