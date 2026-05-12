package com.auction.shared.model;


import java.io.Serializable;

public interface Observer extends Serializable {
    void update ( double newPrice, String Username);
}
