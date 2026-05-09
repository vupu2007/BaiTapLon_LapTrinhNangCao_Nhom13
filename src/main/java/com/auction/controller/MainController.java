package com.auction.controller;

import com.auction.model.Account;
import com.auction.model.Item;
import com.auction.model.User;
import com.auction.service.MainService;
import com.auction.util.CurrentUser;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.List;

public class MainController {

    @FXML private Label balanceLabel;
    @FXML private Label ongoingLabel;
    @FXML private Label wonLabel;
    @FXML private Label welcomeLabel;
    @FXML private VBox hotItemsContainer;


    private MainService mainService;

    public MainController() {
        this.mainService = new MainService();
    }

    @FXML
    public void initialize() {
        Account current = CurrentUser.getUser();
        if (current != null) {
            if (welcomeLabel != null) {
                welcomeLabel.setText("Chào mừng, " + current.getUsername() + "!");
            }
            refreshDashboard();
        }
    }

    private void refreshDashboard() {
        Account current = CurrentUser.getUser();
        // Chỉ hiện balance nếu account đó là User (Bidder/Seller)
        if (current instanceof User) {
            balanceLabel.setText(String.format("%.0f VNĐ", ((User) current).getBalance()));
        } else {
            balanceLabel.setText("N/A");
        }

        ongoingLabel.setText(String.valueOf(mainService.getOngoingCount()));
        wonLabel.setText(String.valueOf(mainService.getWonCount()));
        loadHotAuctions();
    }

    private void loadHotAuctions() {
        hotItemsContainer.getChildren().clear();
        List<Item> items = mainService.getHotAuctions();
        for (Item item : items) {
            VBox card = createItemCard(item);
            hotItemsContainer.getChildren().add(card);
        }
    }

    private VBox createItemCard(Item item) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-border-color: #ddd; -fx-border-radius: 8;");
        Label name = new Label(item.getName());
        Label price = new Label("Giá: " + String.format("%.0f", item.getStartingPrice()) + " VNĐ");
        Button bidButton = new Button("Đấu giá ngay");
        bidButton.setOnAction(e -> System.out.println("Đang đấu giá: " + item.getName()));
        card.getChildren().addAll(name, price, bidButton);
        return card;
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        // 0. Gọi hàm logOut từ class CurrentUser để xóa dữ liệu người dùng hiện tại
        CurrentUser.logOut();

        try {
            // 1. Tải file FXML của màn hình Đăng nhập
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/login.fxml"));
            Parent loginRoot = loader.load();

            // 2. Lấy Stage hiện tại và chuyển Scene (như code cũ của bạn)
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(loginRoot);
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML private VBox roleBox;        // Cái khung trắng bao quanh phần "Tư cách hiện tại"
    @FXML private VBox buyerMenu;      // VBox chứa các nút: Ví tiền, Đang đấu giá...
    @FXML private VBox sellerMenu;     // VBox chứa các nút: Ví tiền, Đang bán, Tạo phiên...
    @FXML private Label lblRoleSidebar; // Nhãn "Người mua" hoặc "Người bán"
    @FXML private Label lblRoleTitle;   // Nhãn chữ nhỏ "Tư cách hiện tại"
    @FXML private MenuButton roleMenuButton;

    /**
     * Chuyển sang giao diện NGƯỜI MUA (Màu Hồng/Tím)
     */
    @FXML
    public void switchToBuyer() {

        // 1. Đổi MenuButton trên Topbar
        roleMenuButton.setText(" Người mua");
        roleMenuButton.setStyle("-fx-background-color: #A21CAF; -fx-background-radius: 10; -fx-text-fill: white;");

        // 2. Đổi màu khung Sidebar bên trái sang màu Hồng nhạt
        roleBox.setStyle("-fx-background-color: #fae8ff; -fx-background-radius: 15; -fx-padding: 20;");

        // 3. Đổi icon và màu chữ trong Sidebar
        lblRoleSidebar.setText("🛒 Người mua");
        lblRoleSidebar.setStyle("-fx-text-fill: #86198f; -fx-font-weight: bold; -fx-font-size: 18;");

        if (lblRoleTitle != null) {
            lblRoleTitle.setStyle("-fx-text-fill: #86198f; -fx-font-size: 11;");
        }

        // 4. Hiển thị menu Người mua, ẩn menu Người bán
        showMenu(true);
    }

    /**
     * Chuyển sang giao diện NGƯỜI BÁN (Màu Xanh)
     */
    @FXML
    public void switchToSeller() {
        // 1. Đổi MenuButton trên Topbar
        roleMenuButton.setText(" Người bán");
        roleMenuButton.setStyle("-fx-background-color: #0284c7; -fx-background-radius: 10; -fx-text-fill: white;");

        // 2. Đổi màu khung Sidebar bên trái sang màu Xanh nhạt
        roleBox.setStyle("-fx-background-color: #e0f2fe; -fx-background-radius: 15; -fx-padding: 20;");

        // 3. Đổi icon và màu chữ trong Sidebar
        lblRoleSidebar.setText("🏪 Người bán");
        lblRoleSidebar.setStyle("-fx-text-fill: #0369a1; -fx-font-weight: bold; -fx-font-size: 18;");

        if (lblRoleTitle != null) {
            lblRoleTitle.setStyle("-fx-text-fill: #0369a1; -fx-font-size: 11;");
        }

        // 4. Hiển thị menu Người bán, ẩn menu Người mua
        showMenu(false);
    }

    private void showMenu(boolean isBuyer) {
        // Menu người mua
        buyerMenu.setVisible(isBuyer);
        buyerMenu.setManaged(isBuyer);

        // Menu người bán (Sẽ hiện thêm nút Đang bán, Tạo phiên)
        sellerMenu.setVisible(!isBuyer);
        sellerMenu.setManaged(!isBuyer);
    }
    @FXML
    private BorderPane mainBorderPane;

    // Hàm xử lý khi nhấn nút "Ví tiền"
    @FXML
    private void showWalletView(ActionEvent event) {
        try {
            // Tải phần "ruột" của Ví tiền (WalletContent.fxml)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/WalletContent.fxml"));
            Node walletNode = loader.load();

            // Chỉ thay đổi vùng trung tâm, thanh Sidebar bên trái sẽ giữ nguyên
            mainBorderPane.setCenter(walletNode);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}