package com.auction.shared.model;


public class Electronics extends Item  {
    private String imagePath;
    private String brand;
    private int warrantyMonths;

    // 1. Constructor rỗng
    public Electronics() {
        super();
    }

    // 2. Constructor đầy đủ
    // THAY ĐỔI: bỏ currentPrice và endTime, thêm categoryId và status
    public Electronics(String itemId, String name, String description,
                       double startingPrice, int ownerId, int categoryId, String status,
                       String brand, int warrantyMonths) {
        super(itemId, name, description, startingPrice, ownerId, categoryId, status);
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public int getWarrantyMonths() { return warrantyMonths; }
    public void setWarrantyMonths(int warrantyMonths) { this.warrantyMonths = warrantyMonths; }

    @Override
    public void printInfo() {
        System.out.println("ID: " + itemId + " | Tên: " + name + " | Giá khởi điểm: " + startingPrice);
        System.out.println("Thương hiệu: " + brand + " | Bảo hành: " + warrantyMonths + " tháng");
    }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
}