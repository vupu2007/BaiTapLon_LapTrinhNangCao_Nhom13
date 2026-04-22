package com.auction.model;

public class Electronics extends Item {

    private String brand;
    private int warrantyMonths;

    public Electronics(String itemId, String name, String description,
                       double startingPrice, String brand, int warrantyMonths) {

        super(itemId, name, description, startingPrice);
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public void printInfo() {

    }
}
