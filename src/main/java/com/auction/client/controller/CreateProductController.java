package com.auction.client.controller;
import com.auction.shared.model.Auction;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class CreateProductController {

    @FXML private TextField productNameField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField startPriceField;

    // Thuộc tính hình ảnh mới
    @FXML private Label lblImagePath;

    // Thuộc tính thời gian dạng bảng chọn mới
    @FXML private DatePicker startDatePicker;
    @FXML private ComboBox<String> startHourCombo;
    @FXML private ComboBox<String> startMinuteCombo;

    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<String> endHourCombo;
    @FXML private ComboBox<String> endMinuteCombo;

    @FXML
    public void initialize() {
        // 1. Khởi tạo danh sách giờ đầy đủ (00 -> 23)
        ObservableList<String> hours = FXCollections.observableArrayList();
        for (int i = 0; i < 24; i++) {
            hours.add(String.format("%02d", i));
        }

        // 2. Khởi tạo danh sách phút ĐẦY ĐỦ từ 00 đến 59 (mỗi bước tăng 1 phút)
        ObservableList<String> minutes = FXCollections.observableArrayList();
        for (int i = 0; i < 60; i++) {
            minutes.add(String.format("%02d", i));
        }

        // Đổ dữ liệu vào các ComboBox trên giao diện
        startHourCombo.setItems(hours);
        endHourCombo.setItems(hours);

        startMinuteCombo.setItems(minutes);
        endMinuteCombo.setItems(minutes);

        // Đặt ngày mặc định cho bảng chọn ngày
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now().plusDays(1)); // Ngày kết thúc mặc định là ngày hôm sau

        // Đặt giờ/phút mặc định xuất hiện sẵn trên giao diện để tránh bị trống ô
        startHourCombo.setValue("08");
        startMinuteCombo.setValue("00");

        endHourCombo.setValue("21");
        endMinuteCombo.setValue("00");
    }

    @FXML
    private void handleUploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn hình ảnh sản phẩm");

        // Chỉ lọc các file ảnh cho người dùng dễ chọn
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        // Mở cửa sổ Windows Explorer để chọn file
        Stage stage = (Stage) lblImagePath.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            // Hiển thị tên file lên giao diện để người dùng biết đã chọn thành công
            lblImagePath.setText(selectedFile.getName());
            System.out.println("Đường dẫn file ảnh tĩnh để xử lý sau: " + selectedFile.getAbsolutePath());
        }
    }

    @FXML
    private void handleCreateAuction() {
        // Hàm này chạy khi bấm nút "Tạo phiên đấu giá"
        String name = productNameField.getText().trim();
        String priceText = startPriceField.getText().trim();

        // Kiểm tra validation cơ bản tránh rỗng hoặc lỗi số
        if (name.isEmpty() || priceText.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Lỗi nhập liệu");
            alert.setContentText("Vui lòng nhập đầy đủ Tên sản phẩm và Giá khởi điểm!");
            alert.showAndWait();
            return;
        }

        double startPrice = 0;
        try {
            startPrice = Double.parseDouble(priceText);
        } catch (NumberFormatException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Lỗi nhập liệu");
            alert.setContentText("Giá khởi điểm phải là một số hợp lệ!");
            alert.showAndWait();
            return;
        }

        // Đọc dữ liệu ngày giờ từ giao diện phục vụ xử lý logic thời gian thực
        LocalDate startDate = startDatePicker.getValue();
        int startHour = Integer.parseInt(startHourCombo.getValue());
        int startMinute = Integer.parseInt(startMinuteCombo.getValue());
        LocalDateTime startTime = LocalDateTime.of(startDate, LocalTime.of(startHour, startMinute));

        // 1. KHỞI TẠO VÀ ĐÓNG GÓI DỮ LIỆU THÀNH ĐỐI TƯỢNG AUCTION THẬT
        Auction newAuction = new Auction();
        newAuction.setItemId(name); // Lấy tên sản phẩm gán vào trường itemId
        newAuction.setStartPrice(startPrice);
        newAuction.setCurrentPrice(startPrice);
        newAuction.setStartTime(startTime);
        newAuction.setStatus(Auction.AuctionStatus.OPEN);

        System.out.println("Tạo phiên thành công: " + name);
        System.out.println("Thời gian bắt đầu xếp lịch: " + startTime);

        // ====================================================================
        // VỊ TRÍ CHUẨN XÁC: Gọi sang MainController để tự vẽ sản phẩm lên trang chủ ngay tức thì
        // ====================================================================
        if (MainController.getInstance() != null) {
            MainController.getInstance().addAuctionToRealtimeUI(newAuction);
        }
        // ====================================================================

        // Hiện thông báo Popup ảo cho ra dáng ứng dụng thật
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText("Tạo phiên đấu giá cho sản phẩm [" + name + "] thành công!");
        alert.showAndWait();

        // Tạo xong thì tự động xóa trắng form nhập liệu hoặc đóng tab tùy logic nhóm
        handleCancel();
    }

    @FXML
    private void handleCancel() {
        // 1. Xóa rỗng các ô nhập văn bản
        productNameField.clear();
        descriptionArea.clear();
        startPriceField.clear();

        // 2. Đặt lại nhãn hình ảnh về trạng thái ban đầu
        lblImagePath.setText("Chưa chọn tệp nào");

        // 3. Đặt lại Ngày về mặc định (Ngày hôm nay và ngày mai)
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now().plusDays(1));

        // 4. Đặt lại Giờ và Phút về giá trị mặc định ban đầu
        startHourCombo.setValue("08");
        startMinuteCombo.setValue("00");

        endHourCombo.setValue("21");
        endMinuteCombo.setValue("00");

        System.out.println("Đã xóa toàn bộ form và đặt lại về mặc định!");
    }
}