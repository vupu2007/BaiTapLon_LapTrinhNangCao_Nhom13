package com.auction.model;

public class Electronics extends Item {
    private String brand;
    private int warrantyMonths;

    // 1. Constructor mặc định (bắt buộc phải có nếu lớp Item có constructor mặc định)
    public Electronics() {
        super();
    }

    // 2. Constructor đầy đủ tham số
    public Electronics(String itemId, String name, String description,
                       double startingPrice, double currentPrice,
                       int ownerId, String endTime,
                       String brand, int warrantyMonths) {

        // Gọi super() với đúng 7 tham số mà lớp Item yêu cầu
        super(itemId, name, description, startingPrice, currentPrice, ownerId, endTime);

        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }

    // --- Getters và Setters cho các thuộc tính riêng của Electronics ---
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public int getWarrantyMonths() { return warrantyMonths; }
    public void setWarrantyMonths(int warrantyMonths) { this.warrantyMonths = warrantyMonths; }

    @Override
    public void printInfo() {
        // Gọi printInfo của lớp cha để in các thông tin chung
        super.printInfo();
        // In thêm thông tin riêng của đồ điện tử
        System.out.println("Thương hiệu: " + brand + " | Bảo hành: " + warrantyMonths + " tháng");
    }
}