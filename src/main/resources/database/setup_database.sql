CREATE DATABASE IF NOT EXISTS online_auction_db;
USE online_auction_db;

 -- 1. Accounts
 CREATE TABLE Accounts (
                          account_id     INT AUTO_INCREMENT PRIMARY KEY,
                          username       VARCHAR(50)  UNIQUE NOT NULL,
                          password       VARCHAR(255) NOT NULL,
                          email          VARCHAR(100),
                          role           ENUM('ADMIN', 'SELLER', 'BIDDER') DEFAULT 'BIDDER',
                          balance        DECIMAL(15, 2) DEFAULT 0.0,
                          total_deposit  DECIMAL(15, 2) DEFAULT 0.0,
                          total_withdraw DECIMAL(15, 2) DEFAULT 0.0,
                          is_locked      BOOLEAN        DEFAULT FALSE,
                          created_at     DATETIME       DEFAULT CURRENT_TIMESTAMP,
                          reset_code VARCHAR(10) DEFAULT NULL
);

 -- 2. Categories
 CREATE TABLE Categories (
                            category_id INT AUTO_INCREMENT PRIMARY KEY,
                            name        VARCHAR(100) NOT NULL
);

 -- 3. Items
 CREATE TABLE Items (
                       item_id       VARCHAR(20) PRIMARY KEY,
                       name          VARCHAR(100) NOT NULL,
                       description   TEXT,
                       starting_price DECIMAL(15, 2) NOT NULL,
                       category_id   INT,
                       owner_id      INT,
                       status        ENUM('AVAILABLE', 'IN_AUCTION', 'SOLD') DEFAULT 'AVAILABLE',
                       image_path    MEDIUMTEXT,
                       attributes    JSON,
                       created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
                       FOREIGN KEY (category_id) REFERENCES Categories(category_id),
                       FOREIGN KEY (owner_id)    REFERENCES Accounts(account_id)
);

 -- 4. Auctions
 CREATE TABLE Auctions (
                          auction_id       INT AUTO_INCREMENT PRIMARY KEY,
                          item_id          VARCHAR(20),
                          seller_id        INT NOT NULL,
                          start_price      DECIMAL(15, 2) NOT NULL,
                          current_price    DECIMAL(15, 2) DEFAULT NULL,
                          min_increment    DECIMAL(15, 2) DEFAULT 1.0,
                          start_time       DATETIME,
                          end_time         DATETIME,
                          original_end_time DATETIME,                          -- Anti-sniping: lưu thời gian kết thúc gốc
                          status           ENUM('OPEN', 'RUNNING', 'FINISHED', 'PAID', 'CANCELED') DEFAULT 'OPEN',
                          winner_id        INT NULL,
                          FOREIGN KEY (item_id)    REFERENCES Items(item_id),
                          FOREIGN KEY (seller_id)  REFERENCES Accounts(account_id),
                          FOREIGN KEY (winner_id)  REFERENCES Accounts(account_id)
);

 -- 5. Bids
 CREATE TABLE Bids (
                      bid_id     INT AUTO_INCREMENT PRIMARY KEY,
                      auction_id INT,
                      bidder_id  INT,
                      bid_amount DECIMAL(15, 2) NOT NULL,
                      bid_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                      FOREIGN KEY (auction_id) REFERENCES Auctions(auction_id),
                      FOREIGN KEY (bidder_id)  REFERENCES Accounts(account_id)
);

 -- 6. AutoBids (Đấu giá tự động)
 CREATE TABLE AutoBids (
                          auto_bid_id INT AUTO_INCREMENT PRIMARY KEY,
                          auction_id  INT NOT NULL,
                          bidder_id   INT NOT NULL,
                          max_bid  DECIMAL(15, 2) NOT NULL,
                          created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
                          FOREIGN KEY (auction_id) REFERENCES Auctions(auction_id),
                          FOREIGN KEY (bidder_id)  REFERENCES Accounts(account_id)
);

 -- 7. Transactions
 CREATE TABLE Transactions (
                              transaction_id INT AUTO_INCREMENT PRIMARY KEY,
                              account_id     INT         NOT NULL,
                              type           VARCHAR(50) NOT NULL,       -- DEPOSIT / WITHDRAW / BID_LOCK / BID_REFUND / ...
                              amount         DOUBLE      NOT NULL,
                              balance_after  DOUBLE    NOT NULL,         -- Số dư sau giao dịch
                              description    VARCHAR(255),
                              created_at     DATETIME DEFAULT CURRENT_TIMESTAMP,
                              FOREIGN KEY (account_id) REFERENCES Accounts(account_id)
);
-- 8. UserBids
CREATE TABLE user_bids (
                           bid_id     INT AUTO_INCREMENT PRIMARY KEY,
                           user_id    INT,
                           item_id    VARCHAR(50),
                           bid_amount DOUBLE,
                           bid_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           status     VARCHAR(20)
);