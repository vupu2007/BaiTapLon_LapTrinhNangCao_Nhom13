package com.auction.service;

import com.auction.model.Item;
import com.auction.model.Electronics;
import java.util.ArrayList;
import java.util.List;

public class MainService {

    // Lấy dữ liệu cho Dashboard
    public double getBalance() {
        return 0.0;
    }

    public int getOngoingCount() {
        return 0;
    }

    public int getWonCount() {
        return 0;
    }

    // Lấy danh sách sản phẩm hot để hiển thị
    public List<Item> getHotAuctions() {
        List<Item> hotItems = new ArrayList<>();

        // Tham số constructor: itemId, name, description, startingPrice, brand, warrantyMonths
// Giả định:
// 1. currentPrice ban đầu = startingPrice
// 2. ownerId = 1 (ID của người đăng)
// 3. endTime = "2026-05-31 23:59:59"

        hotItems.add(new Electronics(
                        "E01",
                        "iPhone 15 Pro Max",
                        "256GB Titan tự nhiên",
                        24500000.0,
                        24500000.0, // Thêm currentPrice
                        1,          // Thêm ownerId
                        "2026-05-31 23:59:59", // Thêm endTime
                        "Apple",
                        12
                ));

        hotItems.add(new Electronics(
                "E02",
                "MacBook Pro M3",
                "16GB 512GB Space Gray",
                38000000.0,
                38000000.0, // Thêm currentPrice
                1,          // Thêm ownerId
                "2026-05-31 23:59:59", // Thêm endTime
                "Apple",
                12
        ));

        hotItems.add(new Electronics(
                "E03",
                "Đồng hồ Rolex",
                "Submariner Date 126610LN",
                120000000.0,
                120000000.0, // Thêm currentPrice
                1,           // Thêm ownerId
                "2026-05-31 23:59:59", // Thêm endTime
                "Rolex",
                60
        ));

        return hotItems;
    }

    // Xử lý logic đăng xuất
    public void logout() {
        System.out.println("Đang đăng xuất khỏi hệ thống...");
    }

}