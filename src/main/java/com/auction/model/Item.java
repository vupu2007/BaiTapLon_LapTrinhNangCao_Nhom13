package com.auction.model;

public class Item implements Entity {
    protected String itemId;
    protected String name;
    protected String description;
    protected double startingPrice;
    protected double currentPrice;
    protected int ownerId;
    protected String endTime;

    // 1. Constructor rỗng
    public Item() {}

    // 2. Constructor đầy đủ (Jeff dùng khi tạo mới từ giao diện)
    public Item(String itemId, String name, String description, double startingPrice, double currentPrice, int ownerId, String endTime) {
        this.itemId = itemId;
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.currentPrice = currentPrice;
        this.ownerId = ownerId;
        this.endTime = endTime;
    }

    @Override
    public String getId() {
        return itemId;
    }

    // --- SETTERS (Để JDBC nhồi dữ liệu vào) ---
    public void setItemId(String itemId) { this.itemId = itemId; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }
    public void setOwnerId(int ownerId) { this.ownerId = ownerId; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    // --- GETTERS (Để lấy dữ liệu ra tính toán/hiển thị) ---
    public String getItemId() { return itemId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getStartingPrice() { return startingPrice; }
    public double getCurrentPrice() { return currentPrice; }
    public int getOwnerId() { return ownerId; }
    public String getEndTime() { return endTime; }

    public void printInfo() {
        System.out.println("ID: " + itemId + " | Tên: " + name + " | Giá hiện tại: " + currentPrice);
    }
}