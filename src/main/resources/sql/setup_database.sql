-- 1. Tạo Database
CREATE DATABASE IF NOT EXISTS online_auction_db;
USE online_auction_db;

-- 2. Bảng Users ( Admin, Seller, Bidder)
CREATE TABLE Users (
                       user_id INT AUTO_INCREMENT PRIMARY KEY,
                       username VARCHAR(50) UNIQUE NOT NULL,
                       password VARCHAR(255) NOT NULL,
                       role ENUM('ADMIN', 'SELLER', 'BIDDER') DEFAULT 'BIDDER'
);

-- 3. Bảng Categories
CREATE TABLE Categories (
                            category_id INT AUTO_INCREMENT PRIMARY KEY,
                            name VARCHAR(100) NOT NULL
);

-- 4. Bảng Items
CREATE TABLE Items (
                       item_id INT AUTO_INCREMENT PRIMARY KEY,
                       name VARCHAR(100) NOT NULL,
                       description TEXT,
                       category_id INT,
                       owner_id INT,
                       FOREIGN KEY (category_id) REFERENCES Categories(category_id),
                       FOREIGN KEY (owner_id) REFERENCES Users(user_id)
);

-- 5. Bảng Auctions (Thêm winner_id, min_increment và các trạng thái chuẩn)
CREATE TABLE Auctions (
                          auction_id INT AUTO_INCREMENT PRIMARY KEY,
                          item_id INT,
                          start_price DECIMAL(10, 2) NOT NULL,
                          current_price DECIMAL(10, 2) DEFAULT 0,
                          min_increment DECIMAL(10, 2) DEFAULT 1.0, -- Bước giá tối thiểu
                          start_time DATETIME,
                          end_time DATETIME,
    -- Trạng thái: OPEN (chưa bắt đầu), RUNNING (đang diễn ra), FINISHED (kết thúc), PAID (đã thanh toán), CANCELED (hủy)
                          status ENUM('OPEN', 'RUNNING', 'FINISHED', 'PAID', 'CANCELED') DEFAULT 'OPEN',
                          winner_id INT NULL,
                          FOREIGN KEY (item_id) REFERENCES Items(item_id),
                          FOREIGN KEY (winner_id) REFERENCES Users(user_id)
);

-- 6. Bảng Bids
CREATE TABLE Bids (
                      bid_id INT AUTO_INCREMENT PRIMARY KEY,
                      auction_id INT,
                      user_id INT,
                      bid_amount DECIMAL(10, 2) NOT NULL,
                      bid_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                      FOREIGN KEY (auction_id) REFERENCES Auctions(auction_id),
                      FOREIGN KEY (user_id) REFERENCES Users(user_id)
);;