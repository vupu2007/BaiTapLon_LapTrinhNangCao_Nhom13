# BaiTapLon_LapTrinhNangCao_Nhom13

> **Đề tài:** Phát triển hệ thống đấu giá trực tuyến
> **Mã lớp:** 2526II_UET.CS2043_8
> **Nhóm:** 13

---

# 1. Giới Thiệu Đề Tài

Hệ thống đấu giá trực tuyến (Online Auction System) là nền tảng cho phép nhiều người dùng cùng tham gia cạnh tranh giá để mua một sản phẩm trong một khoảng thời gian xác định. Thay vì bán với mức giá cố định, người bán sẽ đăng sản phẩm lên hệ thống và người mua sẽ liên tục đặt giá cho đến khi phiên đấu giá kết thúc.

Dự án được xây dựng dựa trên mô hình hoạt động của các nền tảng đấu giá phổ biến như eBay Auctions, đồng thời phát triển thêm nhiều tính năng nâng cao như Auto-Bidding, Anti-sniping và cập nhật dữ liệu theo thời gian thực.

---

# 2. Công Nghệ Sử Dụng

## Ngôn ngữ & Framework

* Java 17+
* JavaFX + FXML
* Maven
* JUnit

## Kiến trúc hệ thống

* Mô hình Client – Server
* Kiến trúc MVC
* Giao tiếp Socket TCP
* Dữ liệu truyền dưới dạng JSON

## Design Patterns áp dụng

* Singleton
* Factory Method
* Observer
* Strategy / Command

## Công cụ hỗ trợ

* GitHub Actions (CI/CD)
* Google Java Style Guide
* IntelliJ IDEA

---

# 3. Cấu Trúc Thư Mục

```text
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

# 4. Hướng Dẫn Chạy Chương Trình

Hệ thống hoạt động theo mô hình Client–Server, vì vậy cần khởi động Server trước rồi mới chạy Client.

## Bước 1: Khởi chạy Server

Mở terminal tại thư mục gốc của dự án và chạy:

```bash
mvn exec:java -Dexec.mainClass="com.auction.server.MainServer"
```

> Lưu ý: Nếu class khởi động Server có tên khác, hãy thay `MainServer` bằng đúng tên class chứa hàm `main()`.

Sau khi khởi động thành công, Server sẽ:

* Kết nối cơ sở dữ liệu.
* Mở Socket lắng nghe kết nối từ Client.
* Khởi chạy các tiến trình quản lý phiên đấu giá.

---

## Bước 2: Khởi chạy Client

Mở terminal thứ hai và thực thi:

```bash
mvn exec:java -Dexec.mainClass="com.auction.client.MainApp"
```

Ứng dụng JavaFX sẽ được mở để người dùng đăng nhập và tham gia hệ thống.

---

## Bước 3: Sử dụng hệ thống

Hệ thống hỗ trợ các vai trò:

* **Bidder:** Tham gia đấu giá sản phẩm.
* **Seller:** Tạo và quản lý phiên đấu giá.
* **Admin:** Quản trị hệ thống.

Dữ liệu đấu giá được cập nhật theo thời gian thực thông qua cơ chế Socket kết hợp Observer Pattern.

---

# 5. Các Chức Năng Đã Hoàn Thành

## 5.1. Chức năng bắt buộc

### Quản lý người dùng

* Đăng ký tài khoản.
* Đăng nhập hệ thống.
* Phân quyền Bidder / Seller / Admin.
* Xác thực thông tin người dùng.

### Quản lý sản phẩm đấu giá

* Thêm sản phẩm đấu giá.
* Chỉnh sửa thông tin sản phẩm.
* Xóa phiên đấu giá.
* Thiết lập:

    * Tên sản phẩm
    * Mô tả
    * Giá khởi điểm
    * Thời gian bắt đầu/kết thúc

### Tham gia đấu giá

* Người dùng đặt giá trực tiếp theo thời gian thực.
* Kiểm tra tính hợp lệ của giá đặt.
* Tự động cập nhật người dẫn đầu.

### Kết thúc phiên tự động

* Tự động đóng phiên khi hết giờ.
* Xác định người chiến thắng.
* Chuyển đổi trạng thái phiên:

```text
OPEN → RUNNING → FINISHED → PAID / CANCELED
```

### Xử lý lỗi tập trung

* Bắt lỗi đặt giá không hợp lệ.
* Ngăn đấu giá khi phiên đã đóng.
* Xử lý lỗi mạng và dữ liệu.
* Quản lý Exception tập trung.

### Giao diện người dùng

* Giao diện xây dựng bằng JavaFX và FXML.
* Hiển thị:

    * Danh sách phiên đấu giá
    * Giá hiện tại
    * Người dẫn đầu
    * Thời gian còn lại

### Xử lý đồng thời (Concurrency)

* Đồng bộ đa luồng an toàn.
* Ngăn race condition.
* Tránh lost update.
* Đảm bảo không xảy ra trường hợp nhiều người cùng thắng.

### Realtime Update

* Áp dụng Observer Pattern.
* Đồng bộ dữ liệu bid theo thời gian thực.
* Không cần polling liên tục.

---

## 5.2. Chức năng nâng cao

### Auto-Bidding

* Thiết lập giá tối đa (`maxBid`).
* Thiết lập bước giá (`increment`).
* Hệ thống tự động trả giá khi có cạnh tranh.
* Sử dụng `PriorityQueue` để xử lý ưu tiên.

### Anti-sniping

* Tự động gia hạn phiên đấu giá nếu xuất hiện bid hợp lệ ở những giây cuối.
* Giúp hạn chế tình trạng “sniping”.

### Biểu đồ biến động giá

* Hiển thị biểu đồ Line Chart theo thời gian thực.
* Theo dõi lịch sử thay đổi giá.
* Cập nhật tự động khi có bid mới.

---

# 6. Tài Nguyên Báo Cáo & Demo

## Báo cáo đồ án

* Google Drive PDF:
  `[Chèn link báo cáo tại đây]`

## Video Demo

* Google Drive Video:
  `[Chèn link demo tại đây]`

## Source Code

* GitHub Repository:
  `[Chèn link GitHub tại đây]`

---

# 7. Thành Viên Nhóm

| STT | Họ và tên    | Vai trò  |
| --- | ------------ | -------- |
| 1   | Thành viên 1 | Leader   |
| 2   | Thành viên 2 | Backend  |
| 3   | Thành viên 3 | Frontend |
| 4   | Thành viên 4 | Testing  |

---

# 8. Ghi Chú

* Dự án yêu cầu Java 17 trở lên.
* Khuyến nghị sử dụng Maven 3.8+.
* Hệ điều hành hỗ trợ: Windows, macOS, Linux.
* Cần khởi động Server trước khi chạy Client.
