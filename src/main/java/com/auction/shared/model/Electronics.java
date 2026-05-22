package com.auction.shared.model;


public class Electronics extends Item  {
    private String brand;
    private int warrantyMonths;

    // 1. Constructor rỗng
    public Electronics() {
        super();
    }

    // 2. Constructor đầy đủ
    public Electronics(String itemId, String name, String description,
                       double startingPrice, int ownerId, int categoryId, String status,
                       String brand, int warrantyMonths) {
        super(itemId, name, description, startingPrice, ownerId, categoryId, status, null);
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
}