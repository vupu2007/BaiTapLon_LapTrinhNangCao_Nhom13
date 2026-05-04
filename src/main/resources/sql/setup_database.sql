-- Tạo Database

CREATE DATABASE IF NOT EXISTS online_auction_db;
USE online_auction_db;

-- 1. Bảng Users
CREATE TABLE Users (
                       user_id INT AUTO_INCREMENT PRIMARY KEY,
                       username VARCHAR(50) UNIQUE NOT NULL,
                       password VARCHAR(255) NOT NULL,
                       email VARCHAR(100) UNIQUE NOT NULL,
                       role ENUM('ADMIN', 'USER') DEFAULT 'USER'
);

-- 2. Bảng Categories
CREATE TABLE Categories (
                            category_id INT AUTO_INCREMENT PRIMARY KEY,
                            name VARCHAR(100) NOT NULL
);

-- 3. Bảng Items
CREATE TABLE Items (
                       item_id INT AUTO_INCREMENT PRIMARY KEY,
                       name VARCHAR(100) NOT NULL,
                       description TEXT,
                       category_id INT,
                       owner_id INT,
                       FOREIGN KEY (category_id) REFERENCES Categories(category_id),
                       FOREIGN KEY (owner_id) REFERENCES Users(user_id)
);

-- 4. Bảng Auctions
CREATE TABLE Auctions (
                          auction_id INT AUTO_INCREMENT PRIMARY KEY,
                          item_id INT,
                          start_price DECIMAL(10, 2) NOT NULL,
                          current_price DECIMAL(10, 2) DEFAULT 0,
                          start_time DATETIME,
                          end_time DATETIME,
                          status VARCHAR(20) CHECK (status IN ('OPEN', 'CLOSED', 'CANCELED')),
                          FOREIGN KEY (item_id) REFERENCES Items(item_id)
);

-- 5. Bảng Bids
CREATE TABLE Bids (
                      bid_id INT AUTO_INCREMENT PRIMARY KEY,
                      auction_id INT,
                      user_id INT,
                      bid_amount DECIMAL(10, 2) NOT NULL,
                      bid_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                      FOREIGN KEY (auction_id) REFERENCES Auctions(auction_id),
                      FOREIGN KEY (user_id) REFERENCES Users(user_id)
);