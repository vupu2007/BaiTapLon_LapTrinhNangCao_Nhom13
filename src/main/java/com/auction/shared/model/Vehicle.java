package com.auction.shared.model;

public class Vehicle extends Item {
    private String brand;       // Hãng xe
    private String model;       // Model xe
    private int year;           // Năm sản xuất
    private int mileage;        // Số km đã đi

    // 1. Constructor rỗng
    public Vehicle() {
        super();
    }

    // 2. Constructor đầy đủ
    public Vehicle(String itemId, String name, String description,
                   double startingPrice, int ownerId, int categoryId, String status,
                   String brand, String model, int year, int mileage) {
        super(itemId, name, description, startingPrice, ownerId, categoryId, status);
        this.brand   = brand;
        this.model   = model;
        this.year    = year;
        this.mileage = mileage;
    }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public int getMileage() { return mileage; }
    public void setMileage(int mileage) { this.mileage = mileage; }

    @Override
    public void printInfo() {
        System.out.println("ID: " + itemId + " | Tên: " + name + " | Giá khởi điểm: " + startingPrice);
        System.out.println("Hãng: " + brand + " | Model: " + model
                + " | Năm: " + year + " | Số km: " + mileage);
    }
}