# Hệ Thống Đấu Giá Trực Tuyến

> **Bài tập lớn — Lập trình Nâng cao | HK II (2526II_UET.CS22043_8)**  
> **Trường Đại học Công nghệ – ĐHQGHN | Nhóm 13 | Năm học 2025–2026**

---

## Bài Toán & Phạm Vi Hệ Thống

### Mục tiêu

**AuctionSystem** mô phỏng quy trình đấu giá điện tử đầy đủ trên nền desktop (Java), lấy cảm hứng từ các hệ thống thực tế như eBay Auctions, đồng thời thực hành toàn diện các kỹ năng lập trình nâng cao:

- **Kiến trúc Client–Server:** Server xử lý toàn bộ nghiệp vụ qua Socket TCP, Client giao diện JavaFX giao tiếp với Server thông qua giao thức Request/Response tuần tự hóa Java.
- **OOP & Design Pattern chuẩn:** phân cấp lớp rõ ràng áp dụng Singleton, Factory Method, Observer đúng ngữ cảnh.
- **An toàn đồng thời:** nhiều Client đặt giá cùng lúc không gây race condition nhờ `ConcurrentHashMap`, `ReentrantLock` và transaction DB.
- **Realtime:** toàn bộ Client đang xem một phiên nhận được cập nhật giá mới ngay lập tức qua cơ chế broadcast Socket (Observer Pattern).
- **Quy trình phát triển chuẩn:** Maven, JUnit 5 + Mockito, GitHub Actions CI/CD.

### Nghiệp vụ

Ba vai trò **Admin**, **Seller** và **Bidder**; luồng nghiệp vụ đầy đủ: đăng ký → đăng nhập → nạp tiền → tạo sản phẩm → tạo phiên đấu giá → đặt bid → tự động kết thúc phiên → thanh toán → công bố kết quả; lịch sử giao dịch minh bạch.


---

## Công Nghệ, Môi Trường & Yêu Cầu Cài Đặt

### Thành phần kỹ thuật

-   Java 21 
-   JavaFX 21.0.3 + FXML + SceneBuilder 
-   Java Socket TCP 
-   MySQL 
-   Maven 
-   JUnit 5
-   CI/CD GitHub Actions

### Môi trường chạy

Ứng dụng hỗ trợ **đa nền tảng ** bao gồm Windows, macOS và Linux.

**Yêu cầu cài đặt trên máy:**

- **JDK 21+**
- **Apache Maven 3.9+**
- **MySQL 8.0+** 
- **Git**

---

## Cấu Trúc Thư Mục

Dự án được tổ chức thành **3 tầng** trong cùng một module Maven:

```
BaiTapLon_LapTrinhNangCao_Nhom13/
├── .github/
│   └── workflows/
│       └── maven.yml
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/auction/
│   │   │       ├── client/
│   │   │       │   ├── controller/
│   │   │       │   ├── network/
│   │   │       │   ├── service/
│   │   │       │   ├── util/
│   │   │       │   └── MainApp.java
│   │   │       │
│   │   │       ├── server/
│   │   │       │   ├── config/
│   │   │       │   ├── dao/
│   │   │       │   ├── network/
│   │   │       │   ├── service/
│   │   │       │   └── util/
│   │   │       │
│   │   │       ├── shared/
│   │   │           ├── exception/
│   │   │       │   ├── model/
│   │   │       │   ├── network/
│   │   │       │   └── util/
│   │   │       │
│   │   │       ├── TestAuth.java
│   │   │       ├── TestAutoBid.java
│   │   │       └── TestScheduler.java
│   │   │
│   │   └── resources/
│   │
│   └── test/
│
├── pom.xml
├── README.md
└── .gitignore
```

---

##  Hướng Dẫn Cài Đặt & Chạy Chương Trình

### Bước 1 — Tải mã nguồn

```bash
git clone https://github.com/<username>/BaiTapLon_LapTrinhNangCao_Nhom13.git
cd BaiTapLon_LapTrinhNangCao_Nhom13
```

### Bước 2 — Khởi tạo cơ sở dữ liệu

Đăng nhập MySQL và chạy script tạo schema:

**Windows (Command Prompt):**
```cmd
mysql -u root -p < src\main\resources\database\setup_database.sql
```

**macOS / Linux (Terminal):**
```bash
mysql -u root -p < src/main/resources/database/setup_database.sql
```

> Hoặc mở file `setup_database.sql` trong MySQL Workbench và thực thi.  
> Script sẽ tạo database `online_auction_db` cùng toàn bộ các bảng cần thiết.

### Bước 3 — Cấu hình kết nối Database

Mở file `src/main/java/com/auction/server/util/DatabaseConnection.java` và chỉnh thông tin kết nối:

```java
config.setJdbcUrl("jdbc:mysql://localhost:3306/online_auction_db?...");
config.setUsername("root");
config.setPassword("YOUR_PASSWORD");   // ← thay bằng mật khẩu MySQL của bạn
```

### Bước 4 — Cấu hình địa chỉ Server cho Client

Mở file `src/main/resources/server.properties`:

```properties
server.host=127.0.0.1    # ← thay bằng IP thực nếu Client và Server chạy trên 2 máy khác nhau
server.port=12345
```

---

## Chạy Server & Client

> **Bắt buộc khởi động Server TRƯỚC, sau đó mới chạy Client.**

### Chạy Server

**Trong IntelliJ IDEA:**
1. Mở file `src/main/java/com/auction/server/network/ServerMain.java`
2. Chuột phải → `Run 'ServerMain'`

**Windows (Command Prompt):**
```cmd
mvn exec:java -Dexec.mainClass="com.auction.server.network.ServerMain"
```

**macOS / Linux (Terminal):**
```bash
mvn exec:java -Dexec.mainClass="com.auction.server.network.ServerMain"
```
```
```
---

### Chạy Client (Giao diện JavaFX)

Mở **terminal/cửa sổ lệnh mới** (giữ terminal Server đang chạy), rồi:

**Trong IntelliJ IDEA (chạy nhiều client cùng lúc):**
1. Vào `Run → Edit Configurations`
2. Chọn cấu hình `MainApp`
3. Tích vào **"Allow multiple instances"**
4. Nhấn Run ▶ nhiều lần — mỗi lần mở 1 cửa sổ client mới

**Windows (Command Prompt):**
```cmd
mvn javafx:run
```

**macOS / Linux (Terminal):**
```bash
mvn javafx:run
```

---

### Chạy Unit Test

**Windows (Command Prompt):**
```cmd
:: Chạy toàn bộ test suite
mvn test -DskipTests=false

**macOS / Linux (Terminal):**
```bash
# Chạy toàn bộ test suite
mvn test -DskipTests=false

```

---

## Danh Sách Chức Năng Đã Hoàn Thành

### Thiết kế lớp & OOP
- Cây kế thừa đầy đủ
-  Nguyên tắc OOP: Encapsulation , Inheritance , Polymorphism , Abstraction 
- Design Pattern: Singleton , Factory Method , Observer (realtime bid notify qua Socket)### Thiết kế lớp & OOP

### Tích hợp, Kiến trúc & Chất lượng mã
- Thiết kế kiến trúc Client-Server với giao tiếp qua mạng Socket TCP
- Mô hình phân tầng MVC: Client dùng JavaFX + FXML; Server chia Controller → Model → DAO- Design Pattern: Singleton , Factory Method , Observer (realtime bid notify qua Socket)
- Sử dụng công cụ build Maven/Gradle
- Sử dụng công cụ build Maven/Gradle
- Thiết lập CI/CD cơ bản (GitHub Actions + test tự động)

### Xác thực & Tài khoản

- Đăng ký tài khoản mới (mặc định vai trò Bidder)
- Đăng nhập / Đăng xuất 
- Cập nhật thông tin cá nhân (username, email)
- Đổi mật khẩu (xác thực mật khẩu cũ trước)
- Chuyển đổi vai trò Bidder ↔ Seller

### Ví Tiền & Giao Dịch

- Nạp tiền vào ví
- Xem số dư hiện tại
- Xem lịch sử giao dịch (nạp tiền, thanh toán đấu giá) lưu MySQL

### Quản Lý Sản Phẩm 

- Thêm sản phẩm mới 
-  Cập nhật thông tin sản phẩm
- Xóa sản phẩm (chỉ khi chưa có phiên đang RUNNING hoặc đã FINISHED)
- Xem danh sách sản phẩm của mình

### Chức Năng Đấu Giá 

-   Tạo phiên đấu giá với giá khởi điểm, bước giá, thời gian bắt đầu/kết thúc
-   Xem danh sách tất cả phiên đang chạy (real-time)
-   Đặt giá thầu thủ công theo thời gian thực qua Socket TCP
-   Realtime Update: broadcast giá mới tức thì đến tất cả Client đang theo dõi phiên (Observer qua Socket)
-   Concurrent Bidding: Kiểm soát đồng thời an toàn kết hợp lock ngăn lost update

### Chức Năng Nâng Cao
-  Auto-Bidding: lọc autoBids từ DB, tìm người có giá thầu phù hợp, tự tạo BidTransaction và broadcast giá mới
-  Anti-sniping: bid trong 60 giây cuối → endTime += 60s, cập nhật DB, broadcast Client; tối đa 5 lần (300 giây)
-  Bid History Visualization: Xem lịch sử toàn bộ lượt đặt giá trong phiên qua biểu đồ trực quan (JavaFX LineChart)

### Kết Thúc Phiên & Thanh Toán Tự Động (AuctionScheduler)

-   Scheduler chạy mỗi 3 giây, tự động chuyển trạng thái OPEN → RUNNING đúng giờ
-   Tự động đóng phiên RUNNING khi hết thời gian → FINISHED
-   Có winner + đủ tiền: trừ tiền Bidder, cộng tiền Seller → item SOLD
-   Không có winner hoặc thanh toán thất bại → item AVAILABLE
-   Thông báo kết quả broadcast đến tất cả Client liên quan

### Xử Lý Lỗi & Ngoại Lệ

-   Kiểm tra tính hợp lệ của giá đấu (chặn đặt giá thấp hơn giá hiện tại, đấu giá khi phiên đã đóng).
-   Xây dựng custom exception và try-catch toàn bộ luồng Socket, đảm bảo hệ thống không sập khi gặp lỗi dữ liệu hoặc mất kết nối

### Quản Trị Hệ Thống (Admin)

-   Dashboard thống kê: tổng số user, phiên đấu giá, doanh thu
-   Quản lý người dùng: xem danh sách, khóa / mở tài khoản
-   Quản lý phiên đấu giá: xem toàn bộ, hủy phiên bất kỳ

### Chức năng thêm
-   Bảo Mật & Xác Thực Nâng Cao: Tích hợp tính năng "Quên mật khẩu" thông qua việc gửi mã OTP 6 chữ số qua Email thực tế, cho phép người dùng đặt lại mật khẩu an toàn.
-   Dashboard Quản Trị Trung Tâm (Admin): Xây dựng một giao diện quản trị tổng quan thống kê số liệu thực tế (tổng số user, tổng phiên đấu giá, doanh thu). Admin có toàn quyền kiểm soát: khóa/mở tài khoản người dùng và can thiệp hủy phiên đấu giá bất kỳ khi có sự cố.



---

## Báo cáo và demo

-   Báo cáo PDF:https://docs.google.com/document/d/1VwJvcxFDPmQwkwSwtXwKnmiYjhUK3nrr/edit?usp=drive_link&ouid=108771013881653690149&rtpof=true&sd=true
-   Video Demo: https://drive.google.com/file/d/1x2K1Iz7SnXxsYM1HTzwS0nc_QHCF8xjp/view?usp=sharing

