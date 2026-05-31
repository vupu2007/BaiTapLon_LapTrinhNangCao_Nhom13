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
                          balance DECIMAL(15, 2) DEFAULT 0.0,
                          created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 3. Bảng Categories (Giữ nguyên)
CREATE TABLE Categories (
                            category_id INT AUTO_INCREMENT PRIMARY KEY,
                            name VARCHAR(100) NOT NULL
);

-- 4. Bảng Items
CREATE TABLE Items (
                       item_id VARCHAR(20) PRIMARY KEY,
                       name VARCHAR(100) NOT NULL,
                       description TEXT,
                       starting_price DECIMAL(15, 2) NOT NULL,
                       category_id INT,
                       owner_id INT,
                       status ENUM('AVAILABLE', 'IN_AUCTION', 'SOLD') DEFAULT 'AVAILABLE',
                       created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                       FOREIGN KEY (category_id) REFERENCES Categories(category_id),
                       FOREIGN KEY (owner_id) REFERENCES Accounts(account_id),
                       attributes JSON
);

-- 5. Bảng Auctions
CREATE TABLE Auctions (
                          auction_id INT AUTO_INCREMENT PRIMARY KEY,
                          item_id VARCHAR(20),
                          seller_id INT NOT NULL,
                          start_price DECIMAL(15, 2) NOT NULL,
                          current_price DECIMAL(15, 2) DEFAULT NULL,
                          min_increment DECIMAL(15, 2) DEFAULT 1.0,
                          start_time DATETIME,
                          end_time DATETIME,
                          status ENUM('OPEN', 'RUNNING', 'FINISHED', 'PAID', 'CANCELED') DEFAULT 'OPEN',
                          winner_id INT NULL,
                          FOREIGN KEY (item_id) REFERENCES Items(item_id),
                          FOREIGN KEY (seller_id) REFERENCES Accounts(account_id),
                          FOREIGN KEY (winner_id) REFERENCES Accounts(account_id)
);

-- 6. Bảng Bids
CREATE TABLE Bids (
                      bid_id INT AUTO_INCREMENT PRIMARY KEY,
                      auction_id INT,
                      bidder_id INT,
                      bid_amount DECIMAL(15, 2) NOT NULL,
                      bid_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                      FOREIGN KEY (auction_id) REFERENCES Auctions(auction_id),
                      FOREIGN KEY (bidder_id) REFERENCES Accounts(account_id)
);

-- 1. Thêm 2 cột mới vào bảng Accounts một cách độc lập
ALTER TABLE Accounts ADD COLUMN total_deposit DECIMAL(15, 2) DEFAULT 0.0;
ALTER TABLE Accounts ADD COLUMN total_withdraw DECIMAL(15, 2) DEFAULT 0.0;

-- 2. Cập nhật giá trị mặc định cho tài khoản 'mhuyen' để tránh bị lỗi NULL dữ liệu
UPDATE Accounts SET total_deposit = 0.0, total_withdraw = 0.0 WHERE username = 'mhuyen';

ALTER TABLE Items ADD COLUMN image_path VARCHAR(255);

ALTER TABLE Items MODIFY COLUMN image_path MEDIUMTEXT;
-- Tạo thêm bản Transactions để lưu trữ số dư
CREATE TABLE Transactions (
                              transaction_id INT AUTO_INCREMENT PRIMARY KEY,
                              account_id INT NOT NULL,
                              type VARCHAR(50) NOT NULL,
                              amount DOUBLE NOT NULL,
                              description VARCHAR(255),
                              created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);