package com.auction.shared.model;

public class Art extends Item {
    private String artist;  // Tên nghệ sĩ
    private int year;       // Năm sáng tác

    // 1. Constructor rỗng
    public Art() {
        super();
    }

    // 2. Constructor đầy đủ
    public Art(String itemId, String name, String description,
               double startingPrice, int ownerId, int categoryId, String status,
               String artist, int year) {
        super(itemId, name, description, startingPrice, ownerId, categoryId, status, null);
        this.artist = artist;
        this.year = year;
    }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    @Override
    public void printInfo() {
        System.out.println("ID: " + itemId + " | Tên: " + name + " | Giá khởi điểm: " + startingPrice);
        System.out.println("Nghệ sĩ: " + artist + " | Năm sáng tác: " + year);
    }
}