-- Tạo database
CREATE DATABASE IF NOT EXISTS auction_db;
USE auction_db;

-- 1. BẢNG USERS (Gộp chung User, Bidder, Seller, Admin)
-- Cách tốt nhất trong SQL để xử lý kế thừa (Inheritance) là thêm 1 cột 'role'
CREATE TABLE users (
                       id VARCHAR(36) PRIMARY KEY, -- Dùng VARCHAR(36) để lưu UUID giống trong code Java
                       username VARCHAR(50) UNIQUE NOT NULL,
                       email VARCHAR(100) UNIQUE NOT NULL,
                       password VARCHAR(255) NOT NULL, -- Trong thực tế sẽ lưu chuỗi Hash
                       role ENUM('BIDDER', 'SELLER', 'ADMIN') NOT NULL,

    -- Các thuộc tính dành riêng cho Bidder (Nếu là Seller/Admin thì để NULL)
                       max_bid DECIMAL(15, 2) DEFAULT NULL,
                       bid_increment DECIMAL(15, 2) DEFAULT NULL,

                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. BẢNG ITEMS (Hàng hóa)
CREATE TABLE items (
                       id VARCHAR(36) PRIMARY KEY,
                       seller_id VARCHAR(36) NOT NULL, -- Người bán tạo ra item này
                       name VARCHAR(150) NOT NULL,
                       description TEXT,
                       starting_price DECIMAL(15, 2) NOT NULL,

    -- Phân loại: Electronics, Art, Vehicle
                       category ENUM('ELECTRONICS', 'ART', 'VEHICLE') NOT NULL,

    -- Dùng JSON để lưu các thuộc tính riêng biệt (mileage, artist, warranty...)
    -- giúp tránh việc tạo quá nhiều bảng con lắt nhắt
                       specific_attributes JSON,

                       FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 3. BẢNG AUCTIONS (Phiên đấu giá)
CREATE TABLE auctions (
                          id VARCHAR(36) PRIMARY KEY,
                          item_id VARCHAR(36) UNIQUE NOT NULL, -- Mỗi phiên đấu giá gắn với 1 Item
                          start_time DATETIME NOT NULL,
                          end_time DATETIME NOT NULL,
                          current_highest_price DECIMAL(15, 2) DEFAULT 0,
                          status ENUM('OPEN', 'RUNNING', 'FINISHED', 'PAID', 'CANCELED') DEFAULT 'OPEN',

    -- Khóa ngoại trỏ đến người đang trả giá cao nhất (có thể NULL lúc mới bắt đầu)
                          highest_bidder_id VARCHAR(36) DEFAULT NULL,

                          FOREIGN KEY (item_id) REFERENCES items(id),
                          FOREIGN KEY (highest_bidder_id) REFERENCES users(id)
);

-- 4. BẢNG BID_TRANSACTIONS (Lịch sử đặt giá)
CREATE TABLE bid_transactions (
                                  id VARCHAR(36) PRIMARY KEY,
                                  auction_id VARCHAR(36) NOT NULL,
                                  bidder_id VARCHAR(36) NOT NULL,
                                  amount DECIMAL(15, 2) NOT NULL,
                                  is_valid BOOLEAN DEFAULT TRUE,
                                  timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                                  FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
                                  FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE
);