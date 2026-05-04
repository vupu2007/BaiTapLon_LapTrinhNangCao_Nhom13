package com.auction.model;

// 1. Tạm bỏ chữ "abstract" để có thể khởi tạo đối tượng bằng new Item()
public class Item implements Entity {
    protected String itemId;
    protected String name;
    protected String description;
    protected double startingPrice;

    // 2. BẮT BUỘC THÊM: Constructor rỗng để Database có thể tạo vỏ đối tượng
    public Item() {
    }

    public Item(String itemId, String name, String description, double startingPrice) {
        this.itemId = itemId;
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
    }

    @Override
    public String getId() {
        return itemId;
    }

    // --- 3. BẮT BUỘC THÊM: Các hàm Setter để nhồi dữ liệu từ MySQL vào ---
    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }

    // --- Các hàm Getter cũ của bạn giữ nguyên ---
    public String getItemId() { return itemId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getStartingPrice() { return startingPrice; }

    // Bỏ abstract của hàm này, viết logic in ra màn hình cơ bản
    public void printInfo() {
        System.out.println("Sản phẩm: " + name + " | Giá khởi điểm: " + startingPrice);
    }
}