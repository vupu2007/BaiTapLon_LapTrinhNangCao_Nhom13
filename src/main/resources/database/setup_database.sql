-- 1. Đồng bộ tên database khớp với application.properties
CREATE DATABASE IF NOT EXISTS auction_db;
USE auction_db;

-- 2. Bảng Accounts
CREATE TABLE Accounts (
                          account_id INT AUTO_INCREMENT PRIMARY KEY,
                          username VARCHAR(50) UNIQUE NOT NULL,
                          password VARCHAR(255) NOT NULL,
                          email VARCHAR(100),
                          role ENUM('ADMIN', 'SELLER', 'BIDDER') DEFAULT 'BIDDER',
                          balance DECIMAL(15, 2) DEFAULT 0.0,
                          total_deposit DECIMAL(15, 2) DEFAULT 0.0,   -- Tích hợp gọn gàng vào lệnh tạo bảng
                          total_withdraw DECIMAL(15, 2) DEFAULT 0.0,  -- Tích hợp gọn gàng vào lệnh tạo bảng
                          created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 3. Bảng Categories
CREATE TABLE Categories (
                            category_id INT AUTO_INCREMENT PRIMARY KEY,
                            name VARCHAR(100) NOT NULL
);

-- 4. Bảng Items (Đồng bộ item_id sang INT để khớp cấu trúc Model ID trong Java)
CREATE TABLE Items (
                       item_id INT AUTO_INCREMENT PRIMARY KEY,
                       name VARCHAR(100) NOT NULL,
                       description TEXT,
                       starting_price DECIMAL(15, 2) NOT NULL,
                       category_id INT,
                       owner_id INT,
                       status ENUM('AVAILABLE', 'IN_AUCTION', 'SOLD') DEFAULT 'AVAILABLE',
                       image_path MEDIUMTEXT,                  -- Tích hợp lưu trữ chuỗi base64 hoặc đường dẫn ảnh dài
                       attributes JSON,
                       created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                       FOREIGN KEY (category_id) REFERENCES Categories(category_id) ON DELETE SET NULL,
                       FOREIGN KEY (owner_id) REFERENCES Accounts(account_id) ON DELETE CASCADE
);

-- 5. Bảng Auctions (Đồng bộ item_id sang INT)
CREATE TABLE Auctions (
                          auction_id INT AUTO_INCREMENT PRIMARY KEY,
                          item_id INT,
                          seller_id INT NOT NULL,
                          start_price DECIMAL(15, 2) NOT NULL,
                          current_price DECIMAL(15, 2) DEFAULT NULL,
                          min_increment DECIMAL(15, 2) DEFAULT 1.0,
                          start_time DATETIME,
                          end_time DATETIME,
                          status ENUM('OPEN', 'RUNNING', 'FINISHED', 'PAID', 'CANCELED') DEFAULT 'OPEN',
                          winner_id INT NULL,
                          FOREIGN KEY (item_id) REFERENCES Items(item_id) ON DELETE CASCADE,
                          FOREIGN KEY (seller_id) REFERENCES Accounts(account_id) ON DELETE CASCADE,
                          FOREIGN KEY (winner_id) REFERENCES Accounts(account_id) ON DELETE SET NULL
);

-- 6. Bảng Bids
CREATE TABLE Bids (
                      bid_id INT AUTO_INCREMENT PRIMARY KEY,
                      auction_id INT,
                      bidder_id INT,
                      bid_amount DECIMAL(15, 2) NOT NULL,
                      bid_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                      FOREIGN KEY (auction_id) REFERENCES Auctions(auction_id) ON DELETE CASCADE,
                      FOREIGN KEY (bidder_id) REFERENCES Accounts(account_id) ON DELETE CASCADE
);

-- 7. Bảng Transactions (Đồng bộ số dư DECIMAL và bổ sung Khóa ngoại)
CREATE TABLE Transactions (
                              transaction_id INT AUTO_INCREMENT PRIMARY KEY,
                              account_id INT NOT NULL,
                              type VARCHAR(50) NOT NULL,
                              amount DECIMAL(15, 2) NOT NULL,          -- Đổi từ DOUBLE sang DECIMAL thống nhất hệ thống
                              description VARCHAR(255),
                              created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                              FOREIGN KEY (account_id) REFERENCES Accounts(account_id) ON DELETE CASCADE
);

-- 8. Chèn dữ liệu mẫu an toàn chống lỗi dữ liệu trống
INSERT INTO Accounts (username, password, email, role, balance, total_deposit, total_withdraw)
VALUES ('mhuyen', '123456', 'mhuyen@auction.com', 'BIDDER', 5000.00, 5000.00, 0.00);