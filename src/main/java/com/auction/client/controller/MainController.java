package com.auction.client.controller;

import com.auction.server.service.MainService;
import com.auction.client.util.CurrentAccount;
import com.auction.shared.model.Auction;
import com.auction.shared.model.Account;
import com.auction.shared.model.User;
import com.auction.shared.model.Item;
import com.auction.shared.model.Electronics;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;

public class MainController {

    @FXML
    private Label balanceLabel, ongoingLabel, wonLabel, welcomeLabel;
    @FXML
    private Button btnFilterAll, btnFilterActive, btnFilterUpcoming;
    @FXML
    private FlowPane flowPane;

    private MainService mainService = new MainService();
    private static MainController instance;
    private String currentFilter = "ALL";
    private final String UPLOAD_DIR = "C:/uet_uploads/"; // Thư mục lưu ảnh thật bên ngoài

    public MainController() {
    }

    @FXML
    public void initialize() {
        instance = this;
        Account current = CurrentAccount.getAccount();
        if (current != null) {
            if (welcomeLabel != null) welcomeLabel.setText("Chào mừng, " + current.getUsername() + "!");
            refreshDashboard();
        }
    }

    public static MainController getInstance() {
        return instance;
    }

    public void refreshDashboard() {
        Account current = CurrentAccount.getAccount();
        if (balanceLabel != null) {
            balanceLabel.setText(current instanceof User ? String.format("%.0f VNĐ", ((User) current).getBalance()) : "N/A");
        }
        if (ongoingLabel != null) ongoingLabel.setText(String.valueOf(mainService.getOngoingCount()));
        if (wonLabel != null) wonLabel.setText(String.valueOf(mainService.getWonCount()));
        handleFilterAll();
    }

    // Tải ảnh ngắn gọn: Ưu tiên quét thư mục upload ngoài trước, lỗi thì về default hệ thống
    private void tryLoadImageToView(ImageView imgView, String preferredFileName) {
        if (preferredFileName != null && preferredFileName.startsWith("base64:")) {
            System.out.println("🖼️ tryLoad: [Chuỗi Base64 ẩn] - Độ dài: " + preferredFileName.length());
            try {
                byte[] bytes = java.util.Base64.getDecoder().decode(preferredFileName.substring(7));
                imgView.setImage(new Image(new java.io.ByteArrayInputStream(bytes)));
                return;
            } catch (Exception e) {
                System.err.println("❌ Lỗi decode Base64: " + e.getMessage());
            }
        } else {
            System.out.println("🖼️ tryLoad: " + preferredFileName);
        }

        if (preferredFileName != null && (preferredFileName.startsWith("http://") || preferredFileName.startsWith("https://"))) {
            try {
                Image img = new Image(preferredFileName, true);
                img.errorProperty().addListener((obs, old, isError) -> {
                    if (isError) System.err.println("❌ Lỗi load URL: " + img.getException().getMessage());
                });
                imgView.setImage(img);
                return;
            } catch (Exception e) {
                System.err.println("❌ Load URL thất bại: " + e.getMessage());
            }
        }

        if (preferredFileName == null || preferredFileName.trim().isEmpty()) {
            preferredFileName = "default.png";
        }

        String[] extensions = {".png", ".jpg", ".jpeg"};
        boolean loaded = false;

        File directFile = new File(UPLOAD_DIR + preferredFileName);
        if (directFile.exists() && directFile.isFile()) {
            imgView.setImage(new Image(directFile.toURI().toString()));
            return;
        }

        for (String ext : extensions) {
            String fullFileName = preferredFileName.contains(".") ? preferredFileName : preferredFileName + ext;
            File extFile = new File(UPLOAD_DIR + fullFileName);
            if (extFile.exists()) {
                imgView.setImage(new Image(extFile.toURI().toString()));
                loaded = true;
                break;
            }
        }

        if (!loaded) {
            for (String ext : extensions) {
                String fullFileName = preferredFileName.contains(".") ? preferredFileName : preferredFileName + ext;
                java.io.InputStream is = getClass().getResourceAsStream("/com/auction/client/images/" + fullFileName);
                if (is != null) {
                    imgView.setImage(new Image(is));
                    loaded = true;
                    break;
                }
            }
        }

        if (!loaded) {
            java.io.InputStream defaultIs = getClass().getResourceAsStream("/com/auction/client/images/default.png");
            if (defaultIs != null) imgView.setImage(new Image(defaultIs));
        }
    }

    @FXML
    private void handleFilterAll() {
        currentFilter = "ALL";
        setButtonActive(btnFilterAll);
        if (flowPane == null) return;
        flowPane.getChildren().clear();
        for (Item item : mainService.getHotAuctions()) {
            flowPane.getChildren().add(createItemCardWithStatus(item, "Đang diễn ra"));
        }
    }

    @FXML
    private void handleFilterActive() {
        currentFilter = "ACTIVE";
        setButtonActive(btnFilterActive);
        if (flowPane == null) return;
        flowPane.getChildren().clear();
        for (Item item : mainService.getHotAuctions()) {
            flowPane.getChildren().add(createItemCardWithStatus(item, "Đang diễn ra"));
        }
    }

    @FXML
    private void handleFilterUpcoming() {
        currentFilter = "UPCOMING";
        setButtonActive(btnFilterUpcoming);
        if (flowPane == null) return;
        flowPane.getChildren().clear();
        for (Item item : mainService.getHotAuctions()) {
            flowPane.getChildren().add(createItemCardWithStatus(item, "Sắp diễn ra"));
        }
    }

    private VBox createItemCardWithStatus(Item item, String statusText) {
        VBox card = new VBox();
        card.setPrefWidth(300);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-border-width: 1;");

        StackPane imageHolder = new StackPane();
        imageHolder.setPrefHeight(180);
        Region bgRegion = new Region();
        bgRegion.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 11 11 0 0;");
        imageHolder.getChildren().add(bgRegion);

        ImageView imgView = new ImageView();
        imgView.setFitWidth(300);
        imgView.setFitHeight(180);
        imgView.setPreserveRatio(false);

        String preferredName = item.getImagePath();

        if (preferredName == null || preferredName.trim().isEmpty()) {
            preferredName = "default.png";
            if (item instanceof Electronics) {
                String brand = ((Electronics) item).getBrand();
                if (brand != null && !brand.trim().isEmpty()) preferredName = brand.trim();
            }
        }
        tryLoadImageToView(imgView, preferredName);

        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(300, 180);
        clip.setArcWidth(15);
        clip.setArcHeight(15);
        imgView.setClip(clip);
        imageHolder.getChildren().add(imgView);

        Label statusLabel = new Label(statusText);
        statusLabel.setStyle(statusText.equals("Sắp diễn ra") ?
                "-fx-background-color: #dbeafe; -fx-text-fill: #2563eb; -fx-background-radius: 20; -fx-font-weight: bold;" :
                "-fx-background-color: #dcfce7; -fx-text-fill: #16a34a; -fx-background-radius: 20; -fx-font-weight: bold warm-white;");
        statusLabel.setPadding(new Insets(5, 12, 5, 12));
        StackPane.setAlignment(statusLabel, Pos.TOP_RIGHT);
        StackPane.setMargin(statusLabel, new Insets(10, 10, 0, 0));
        imageHolder.getChildren().add(statusLabel);

        VBox infoBox = new VBox(8);
        infoBox.setPadding(new Insets(15));
        Label nameLabel = new Label(item.getName());
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        nameLabel.setTextFill(javafx.scene.paint.Color.valueOf("#1e293b"));
        Label descLabel = new Label(item.getDescription() != null && !item.getDescription().isEmpty() ? item.getDescription() : "Sản phẩm chất lượng cao đang trong phiên đấu giá công khai.");
        descLabel.setPrefHeight(40);
        descLabel.setTextFill(javafx.scene.paint.Color.valueOf("#64748b"));
        descLabel.setWrapText(true);

        Region spacer = new Region();
        spacer.setPrefHeight(10);
        HBox priceBox = new HBox();
        priceBox.setAlignment(Pos.CENTER_LEFT);
        Label priceTitle = new Label("Giá hiện tại:");
        priceTitle.setTextFill(javafx.scene.paint.Color.valueOf("#64748b"));
        Region priceSpacer = new Region();
        HBox.setHgrow(priceSpacer, Priority.ALWAYS);
        Label priceValue = new Label(String.format("%,.0f đ", item.getStartingPrice()));
        priceValue.setFont(Font.font("System", FontWeight.BOLD, 16));
        priceValue.setTextFill(javafx.scene.paint.Color.valueOf("#0284c7"));
        priceBox.getChildren().addAll(priceTitle, priceSpacer, priceValue);

        Button bidButton = new Button(statusText.equals("Sắp diễn du") ? "Xem chi tiết" : "Đấu giá ngay");
        bidButton.setMaxWidth(Double.MAX_VALUE);
        bidButton.setStyle("-fx-background-color: #0ea5e9; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: bold; -fx-cursor: hand;");
        bidButton.setPadding(new Insets(8, 0, 8, 0));
        bidButton.setOnAction(e -> showAuctionDetail(item));

        infoBox.getChildren().addAll(nameLabel, descLabel, spacer, priceBox, bidButton);
        card.getChildren().addAll(imageHolder, infoBox);
        return card;
    }

    public void addAuctionToRealtimeUI(Auction newAuction) {
        Platform.runLater(() -> {
            if (flowPane != null && (currentFilter.equals("ALL") || currentFilter.equals("UPCOMING"))) {
                VBox card = createCardFromAuction(newAuction);
                flowPane.getChildren().add(0, card);
            }
        });
    }

    private VBox createCardFromAuction(Auction auction) {
        VBox card = new VBox();
        card.setPrefWidth(300);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-border-width: 1;");

        StackPane imageHolder = new StackPane();
        imageHolder.setPrefHeight(180);
        Region bgRegion = new Region();
        bgRegion.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 11 11 0 0;");
        imageHolder.getChildren().add(bgRegion);

        ImageView imgView = new ImageView();
        imgView.setFitWidth(300);
        imgView.setFitHeight(180);
        imgView.setPreserveRatio(false);

        String preferredName = null;
        try {
            java.lang.reflect.Method getImgMethod = auction.getClass().getMethod("getImage");
            preferredName = (String) getImgMethod.invoke(auction);
        } catch (Exception e) {
            try {
                java.lang.reflect.Method getImgUrlMethod = auction.getClass().getMethod("getImageUrl");
                preferredName = (String) getImgUrlMethod.invoke(auction);
            } catch (Exception ex) {
                preferredName = auction.getItemId();
            }
        }

        if (preferredName == null || preferredName.trim().isEmpty()) {
            preferredName = "default.png";
        }
        tryLoadImageToView(imgView, preferredName);

        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(300, 180);
        clip.setArcWidth(15);
        clip.setArcHeight(15);
        imgView.setClip(clip);
        imageHolder.getChildren().add(imgView);

        Label statusLabel = new Label("Đang diễn ra");
        statusLabel.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #16a34a; -fx-background-radius: 20; -fx-font-weight: bold;");
        statusLabel.setPadding(new Insets(5, 12, 5, 12));
        StackPane.setAlignment(statusLabel, Pos.TOP_RIGHT);
        StackPane.setMargin(statusLabel, new Insets(10, 10, 0, 0));
        imageHolder.getChildren().add(statusLabel);

        VBox infoBox = new VBox(8);
        infoBox.setPadding(new Insets(15));
        Label nameLabel = new Label(auction.getProductName() != null ? auction.getProductName() : "Sản phẩm mới lên sàn");
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        nameLabel.setTextFill(javafx.scene.paint.Color.valueOf("#1e293b"));
        Label descLabel = new Label("Sản phẩm chất lượng cao đang trong phiên đấu giá công khai.");
        descLabel.setPrefHeight(40);
        descLabel.setTextFill(javafx.scene.paint.Color.valueOf("#64748b"));
        descLabel.setWrapText(true);

        Region spacer = new Region();
        spacer.setPrefHeight(10);
        HBox priceBox = new HBox();
        priceBox.setAlignment(Pos.CENTER_LEFT);
        Label priceTitle = new Label("Giá hiện tại:");
        priceTitle.setTextFill(javafx.scene.paint.Color.valueOf("#64748b"));
        Region priceSpacer = new Region();
        HBox.setHgrow(priceSpacer, Priority.ALWAYS);
        Label priceValue = new Label(String.format("%,.0f đ", auction.getStartPrice()));
        priceValue.setFont(Font.font("System", FontWeight.BOLD, 16));
        priceValue.setTextFill(javafx.scene.paint.Color.valueOf("#0284c7"));
        priceBox.getChildren().addAll(priceTitle, priceSpacer, priceValue);

        Button bidButton = new Button("Đấu giá ngay");
        bidButton.setMaxWidth(Double.MAX_VALUE);
        bidButton.setStyle("-fx-background-color: #0ea5e9; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: bold; -fx-cursor: hand;");
        bidButton.setPadding(new Insets(8, 0, 8, 0));
        bidButton.setOnAction(e -> showAuctionDetail(auction));

        infoBox.getChildren().addAll(nameLabel, descLabel, spacer, priceBox, bidButton);
        card.getChildren().addAll(imageHolder, infoBox);
        return card;
    }

    private void setButtonActive(Button activeButton) {
        Button[] filterButtons = {btnFilterAll, btnFilterActive, btnFilterUpcoming};
        for (Button btn : filterButtons) {
            if (btn != null)
                btn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 0 20;");
        }
        if (activeButton != null)
            activeButton.setStyle("-fx-background-color: #0ea5e9; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 0 20;");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        CurrentAccount.logOut();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/LoginView.fxml"));
            ((Stage) ((Node) event.getSource()).getScene().getWindow()).getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ================= 🔥 HÀM CHUYỂN TRANG CHI TIẾT AN TOÀN - TRUY VẤN NGƯỢC DATABASE =================
    public void showAuctionDetail(Object productData) {
        try {
            java.net.URL fxmlLocation = getClass().getResource("/view/AuctionDetailView.fxml");
            if (fxmlLocation == null) fxmlLocation = getClass().getResource("/view/AuctionDetail.fxml");
            if (fxmlLocation == null) fxmlLocation = getClass().getResource("/com/auction/client/view/AuctionDetailView.fxml");
            if (fxmlLocation == null) fxmlLocation = getClass().getResource("/com/auction/client/view/AuctionDetail.fxml");
            if (fxmlLocation == null) fxmlLocation = getClass().getResource("AuctionDetailView.fxml");

            if (fxmlLocation == null) {
                System.err.println("❌ KHÔNG TÌM THẤY FILE FXML TRANG CHI TIẾT!");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent detailView = loader.load();
            AuctionDetailController detailController = loader.getController();

            if (detailController != null) {
                String name = "Sản phẩm";
                String priceText = "0 đ";
                String rawImageSource = "default.png";
                String description = "Sản phẩm chất lượng cao đang trong phiên đấu giá.";
                String sellerName = "Hệ thống đấu giá";

                // Trường hợp 1: Nhấn vào Item tĩnh truyền thống từ DB
                if (productData instanceof Item) {
                    Item item = (Item) productData;
                    name = item.getName();
                    priceText = String.format("%,.0f đ", item.getStartingPrice());
                    rawImageSource = item.getImagePath();
                    if (item.getDescription() != null && !item.getDescription().trim().isEmpty()) {
                        description = item.getDescription();
                    }
                }
                // 🔥 Trường hợp 2: Nhấn từ gói Real-time (Auction) -> Truy vết ngược lại DB để lấy thông tin đăng bán
                else if (productData instanceof Auction) {
                    Auction auction = (Auction) productData;

                    // Lấy thông tin cơ bản trước từ phiên Real-time để phòng hờ
                    name = auction.getProductName() != null ? auction.getProductName() : "Sản phẩm mới";
                    priceText = String.format("%,.0f đ", auction.getStartPrice());

                    // Trích xuất chuỗi ảnh từ gói tin Realtime Auction
                    try {
                        java.lang.reflect.Method getImgMethod = auction.getClass().getMethod("getImage");
                        rawImageSource = (String) getImgMethod.invoke(auction);
                    } catch (Exception e) {
                        try {
                            java.lang.reflect.Method getImgUrlMethod = auction.getClass().getMethod("getImageUrl");
                            rawImageSource = (String) getImgUrlMethod.invoke(auction);
                        } catch (Exception ex) {
                            rawImageSource = auction.getItemId();
                        }
                    }

                    // 💡 BƯỚC THẦN THÁNH: Tìm kiếm Item gốc từ mainService theo ID để lấy lại Mô tả & Giá đăng bán gốc
                    Item dbItem = null;
                    if (mainService != null) {
                        try {
                            // Cách 1: Tìm kiếm trong danh sách Hot Auctions đang có sẵn ở MainService của má
                            if (mainService.getHotAuctions() != null) {
                                for (Item it : mainService.getHotAuctions()) {
                                    if (String.valueOf(it.getId()).equals(auction.getItemId())) {
                                        dbItem = it;
                                        break;
                                    }
                                }
                            }

                            // Cách 2: Nếu chưa tìm thấy, cố gắng thử gọi hàm getItemById nếu Service của má có hỗ trợ
                            if (dbItem == null) {
                                try {
                                    java.lang.reflect.Method getByIdMethod = mainService.getClass().getMethod("getItemById", int.class);
                                    dbItem = (Item) getByIdMethod.invoke(mainService, Integer.parseInt(auction.getItemId()));
                                } catch (Exception e2) {
                                    try {
                                        java.lang.reflect.Method getByIdMethodStr = mainService.getClass().getMethod("getItemById", String.class);
                                        dbItem = (Item) getByIdMethodStr.invoke(mainService, auction.getItemId());
                                    } catch (Exception e3) { /* Không có hàm này thì thôi bỏ qua */ }
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("⚠️ Lỗi khi truy quét Item gốc từ DB: " + e.getMessage());
                        }
                    }

                    // Nếu tìm được Item gốc đăng bán trong DB, ghi đè toàn bộ dữ liệu tĩnh chuẩn chỉnh lên màn hình!
                    if (dbItem != null) {
                        name = dbItem.getName();
                        priceText = String.format("%,.0f đ", dbItem.getStartingPrice()); // Đây chính là Giá Khởi Điểm lúc đăng bán!
                        if (dbItem.getDescription() != null && !dbItem.getDescription().trim().isEmpty()) {
                            description = dbItem.getDescription();
                        }
                    }
                }

                if (rawImageSource == null || rawImageSource.trim().isEmpty() || rawImageSource.equals("null")) {
                    rawImageSource = "default.png";
                }

                // 🔥 DÙNG REFLECTION ĐỂ ÉP ĐỒNG BỘ CÁC LABEL CON TRONG CHI TIẾT (Xóa sổ chữ "Đang tải...", "0 đ")
                try {
                    // 1. Ép hiển thị mô tả sản phẩm thật
                    java.lang.reflect.Field descField = detailController.getClass().getDeclaredField("lblInfoDescription");
                    descField.setAccessible(true);
                    Label lblDesc = (Label) descField.get(detailController);
                    if (lblDesc != null) lblDesc.setText(description);
                } catch (Exception e) {
                    if (detailController.lblInfoDescription != null) detailController.lblInfoDescription.setText(description);
                }

                try {
                    // 2. Ép nhãn Tên Sản Phẩm ở phần Chi tiết dưới
                    java.lang.reflect.Field infoNameField = detailController.getClass().getDeclaredField("lblInfoName");
                    infoNameField.setAccessible(true);
                    Label lblInfoName = (Label) infoNameField.get(detailController);
                    if (lblInfoName != null) lblInfoName.setText(name);
                } catch (Exception e) {}

                try {
                    // 3. Quét và ép nhãn Giá Khởi Điểm ở bảng chi tiết dưới ăn theo priceText (Giá lúc đăng bán)
                    String[] priceFields = {"lblInfoStartPrice", "lblStartingPrice", "lblStartPriceDetail", "lblStartPrice"};
                    for (String fieldName : priceFields) {
                        try {
                            java.lang.reflect.Field pField = detailController.getClass().getDeclaredField(fieldName);
                            pField.setAccessible(true);
                            Label lblPrice = (Label) pField.get(detailController);
                            if (lblPrice != null) {
                                lblPrice.setText(priceText);
                            }
                        } catch (NoSuchFieldException ex) {}
                    }
                } catch (Exception e) {}

                try {
                    // 4. Ép nhãn Người Bán hiển thị thông tin thay vì treo "Đang tải..."
                    java.lang.reflect.Field sellerField = detailController.getClass().getDeclaredField("lblInfoSeller");
                    if (sellerField != null) {
                        sellerField.setAccessible(true);
                        Label lblSeller = (Label) sellerField.get(detailController);
                        if (lblSeller != null) lblSeller.setText(sellerName);
                    }
                } catch (Exception e) {}

                // Gọi hàm khởi tạo giao diện gốc của má
                System.out.println("🚀 Đang đồng bộ dữ liệu chuẩn sang trang chi tiết...");
                detailController.initData(name, priceText, null, rawImageSource);
            }

            if (MainLayoutController.getInstance() != null) {
                MainLayoutController.getInstance().setContent(detailView);
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi nghiêm trọng khi tải trang chi tiết: " + e.getMessage());
            e.printStackTrace();
        }
    }
}