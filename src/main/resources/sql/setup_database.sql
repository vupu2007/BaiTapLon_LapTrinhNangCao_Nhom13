-- 1. Tạo Database
CREATE DATABASE IF NOT EXISTS online_auction_db;
USE online_auction_db;

-- 2. Bảng Accounts (Thay thế cho bảng Users để khớp với Class Account trong Java)
CREATE TABLE Accounts (
                          account_id INT AUTO_INCREMENT PRIMARY KEY,
                          username VARCHAR(50) UNIQUE NOT NULL,
                          password VARCHAR(255) NOT NULL,
                          email VARCHAR(100),
                          role ENUM('ADMIN', 'SELLER', 'BIDDER') DEFAULT 'BIDDER',
                          balance DECIMAL(15, 2) DEFAULT 0.0
);

-- 3. Bảng Categories (Giữ nguyên)
CREATE TABLE Categories (
                            category_id INT AUTO_INCREMENT PRIMARY KEY,
                            name VARCHAR(100) NOT NULL
);

-- 4. Bảng Items
CREATE TABLE Items (
                       item_id VARCHAR(20) PRIMARY KEY, -- Ví dụ: 'EL001'
                       name VARCHAR(100) NOT NULL,
                       description TEXT,
                       starting_price DECIMAL(15, 2) NOT NULL,
                       category_id INT,
                       owner_id INT, -- Liên kết với account_id của người bán
                       status VARCHAR(20) DEFAULT 'ACTIVE',
                       FOREIGN KEY (category_id) REFERENCES Categories(category_id),
                       FOREIGN KEY (owner_id) REFERENCES Accounts(account_id) -- Đổi sang Accounts
);

-- 5. Bảng Auctions
CREATE TABLE Auctions (
                          auction_id INT AUTO_INCREMENT PRIMARY KEY,
                          item_id VARCHAR(20),
                          start_price DECIMAL(15, 2) NOT NULL,
                          current_price DECIMAL(15, 2) DEFAULT 0,
                          min_increment DECIMAL(15, 2) DEFAULT 1.0,
                          start_time DATETIME,
                          end_time DATETIME,
                          status ENUM('OPEN', 'RUNNING', 'FINISHED', 'PAID', 'CANCELED') DEFAULT 'OPEN',
                          winner_id INT NULL, -- Liên kết với account_id của người thắng
                          FOREIGN KEY (item_id) REFERENCES Items(item_id),
                          FOREIGN KEY (winner_id) REFERENCES Accounts(account_id) -- Đổi sang Accounts
);

-- 6. Bảng Bids
CREATE TABLE Bids (
                      bid_id INT AUTO_INCREMENT PRIMARY KEY,
                      auction_id INT,
                      bidder_id INT, -- Đổi user_id thành bidder_id cho rõ nghĩa
                      bid_amount DECIMAL(15, 2) NOT NULL,
                      bid_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                      FOREIGN KEY (auction_id) REFERENCES Auctions(auction_id),
                      FOREIGN KEY (bidder_id) REFERENCES Accounts(account_id) -- Đổi sang Accounts
);