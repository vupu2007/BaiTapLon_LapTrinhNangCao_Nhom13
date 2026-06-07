package com.auction.client.controller;

import com.auction.client.service.RoleService;
import com.auction.client.network.ClientSocket;
import com.auction.shared.model.Account;
import com.auction.shared.model.Bidder;
import com.auction.shared.model.Seller;
import com.auction.shared.network.MessageType;
import com.auction.shared.network.Request;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import com.auction.client.util.CurrentAccount;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MainLayoutController {

    @FXML private StackPane contentArea;
    @FXML private VBox buyerMenu, sellerMenu, roleBox;
    @FXML private Label lblRoleSidebar, lblClock, nameLabel;
    @FXML private MenuButton roleMenuButton;

    @FXML private Button btnHome, btnWallet, btnAuction, btnSelling,
            btnCreateAuction, btnHistory, btnSettings;
    @FXML private Button btnContract;
    @FXML private Button btnAdminPanel;

    private Timeline clockTimeline;
    private final RoleService roleService = new RoleService();

    @FXML
    public void initialize() {
        if (CurrentAccount.getAccount() != null) {
            nameLabel.setText("👤 " + CurrentAccount.getAccount().getUsername());
        }
        startRealtimeClock();
        openHome();
    }

    private void startRealtimeClock() {
        if (clockTimeline != null) clockTimeline.stop();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            if (lblClock != null) lblClock.setText(LocalDateTime.now().format(formatter));
        }));
        clockTimeline.setCycleCount(Animation.INDEFINITE);
        clockTimeline.play();
    }

    private void loadPageAsync(String fxmlPath) {
        Task<Parent> loadTask = new Task<>() {
            @Override
            protected Parent call() throws Exception {
                URL fxmlUrl = getClass().getResource(fxmlPath);
                if (fxmlUrl == null)
                    throw new IllegalArgumentException("Không tìm thấy FXML: " + fxmlPath);
                return new FXMLLoader(fxmlUrl).load();
            }
        };

        loadTask.setOnSucceeded(e -> {
            Parent page = loadTask.getValue();
            if (contentArea != null && page != null) {
                contentArea.getChildren().clear();
                if (page instanceof Region region) {
                    region.prefWidthProperty().bind(contentArea.widthProperty());
                    region.prefHeightProperty().bind(contentArea.heightProperty());
                    region.setMaxWidth(Double.MAX_VALUE);
                    region.setMaxHeight(Double.MAX_VALUE);
                }
                StackPane.setAlignment(page, Pos.TOP_CENTER);
                contentArea.getChildren().add(page);
            }
        });

        loadTask.setOnFailed(e ->
                System.err.println("❌ Lỗi tải giao diện: " + fxmlPath
                        + " -> " + loadTask.getException().getMessage()));

        Thread thread = new Thread(loadTask);
        thread.setDaemon(true);
        thread.start();
    }

    public void setContent(Node content) {
        if (contentArea != null && content != null) {
            if (Platform.isFxApplicationThread()) {
                contentArea.getChildren().setAll(content);
            } else {
                Platform.runLater(() -> contentArea.getChildren().setAll(content));
            }
        }
    }

    private void setActive(Button activeButton) {
        Button[] buttons = {btnHome, btnWallet, btnAuction, btnSelling,
                btnCreateAuction, btnHistory, btnContract,
                btnSettings, btnAdminPanel};
        for (Button btn : buttons) {
            if (btn != null) {
                btn.getStyleClass().remove("nav-button-active");
                if (!btn.getStyleClass().contains("nav-button"))
                    btn.getStyleClass().add("nav-button");
            }
        }
        if (activeButton != null) activeButton.getStyleClass().add("nav-button-active");
    }

    public void openAuctionDetailWithObject(Object data) {
        if (data == null) return;
        String path = getClass().getResource("/view/AuctionDetailView.fxml") != null
                ? "/view/AuctionDetailView.fxml" : "/view/AuctionDetail.fxml";
        setActive(btnAuction);

        Task<Parent> loadTask = new Task<>() {
            @Override
            protected Parent call() throws Exception {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
                Parent page = loader.load();
                AuctionDetailController ctrl = loader.getController();
                if (ctrl != null) {
                    if (data instanceof com.auction.shared.model.Auction a)
                        ctrl.loadProductDetail(a);
                    else if (data instanceof com.auction.shared.model.Item i)
                        ctrl.loadProductDetail(i);
                }
                return page;
            }
        };

        loadTask.setOnSucceeded(e -> setContent(loadTask.getValue()));
        loadTask.setOnFailed(e ->
                System.err.println("❌ Không thể nạp trang chi tiết: "
                        + loadTask.getException().getMessage()));

        Thread thread = new Thread(loadTask);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML public void openHome()          { setActive(btnHome);          loadPageAsync("/view/MainView.fxml"); }
    @FXML public void openSelling()       { setActive(btnSelling);       loadPageAsync("/view/MyProducts.fxml"); }
    @FXML private void openWallet()       { setActive(btnWallet);        loadPageAsync("/view/WalletView.fxml"); }
    @FXML private void openAuction()      { setActive(btnAuction);       loadPageAsync("/view/ActiveAuctions.fxml"); }
    @FXML private void openCreateAuction(){ setActive(btnCreateAuction); loadPageAsync("/view/CreateAuction.fxml"); }
    @FXML private void openHistory()      { setActive(btnHistory);       loadPageAsync("/view/HistoryView.fxml"); }
    @FXML private void openSettings()     { setActive(btnSettings);      loadPageAsync("/view/Settings.fxml"); }
    @FXML private void openContract()     { setActive(btnContract);      loadPageAsync("/view/ContractView.fxml"); }
    public void showCreateProductView()   { openCreateAuction(); }

    @FXML
    private void switchToBuyer() {
        Account current = CurrentAccount.getAccount();
        if (current == null) return;

        // ✅ Controller không biết gì về Request/MessageType
        roleService.switchRoleAsync(Integer.parseInt(current.getId()), "BIDDER", resp -> {
            if (resp != null && resp.isSuccess()) {
                Bidder bidder = new Bidder(current.getId(), current.getUsername(),
                        current.getPassword(), current.getEmail(),
                        current.getBalance() != null ? current.getBalance() : 0.0);
                CurrentAccount.setAccount(bidder);
                updateRoleUI("role-buyer", "🛒 Người mua", true, false);
                openHome();
            }
        });
    }

    @FXML
    private void switchToSeller() {
        Account current = CurrentAccount.getAccount();
        if (current == null) return;

        // ✅ Controller không biết gì về Request/MessageType
        roleService.switchRoleAsync(Integer.parseInt(current.getId()), "SELLER", resp -> {
            if (resp != null && resp.isSuccess()) {
                Seller seller = new Seller(current.getId(), current.getUsername(),
                        current.getPassword(), current.getEmail(),
                        current.getBalance() != null ? current.getBalance() : 0.0);
                CurrentAccount.setAccount(seller);
                updateRoleUI("role-seller", "🏪 Người bán", false, true);
                openHome();
            }
        });
    }

    private void updateRoleUI(String cssClass, String roleText,
                              boolean showBuyer, boolean showSeller) {
        if (lblRoleSidebar != null) lblRoleSidebar.setText(roleText);
        if (roleMenuButton != null) roleMenuButton.setText("🔄 " + roleText.substring(2));
        if (roleBox != null) {
            roleBox.getStyleClass().removeAll("role-buyer", "role-seller");
            roleBox.getStyleClass().add(cssClass);
        }
        if (buyerMenu != null) { buyerMenu.setVisible(showBuyer);  buyerMenu.setManaged(showBuyer); }
        if (sellerMenu != null) { sellerMenu.setVisible(showSeller); sellerMenu.setManaged(showSeller); }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        Account current = CurrentAccount.getAccount();

        // ✅ Nếu là Seller thì switch role về BIDDER trước khi logout — dùng service
        if (current instanceof Seller) {
            roleService.switchRoleAsync(Integer.parseInt(current.getId()), "BIDDER", resp -> {});
        }

        if (clockTimeline != null) clockTimeline.stop();

        try {
            CurrentAccount.setAccount(null);
            Parent root = FXMLLoader.load(getClass().getResource("/view/LoginView.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Đăng nhập");
        } catch (IOException e) {
            System.err.println("❌ Lỗi đăng xuất: " + e.getMessage());
        }
    }

    public void setAdminMode(boolean isAdmin) {
        if (btnAdminPanel != null) {
            btnAdminPanel.setVisible(isAdmin);
            btnAdminPanel.setManaged(isAdmin);
        }
    }

    @FXML
    private void openAdminPanel() {
        setActive(btnAdminPanel);
        try {
            String[] paths = {
                    "/com/auction/client/view/AdminLayout.fxml",
                    "/view/AdminLayoutView.fxml",
                    "/view/AdminLayout.fxml"
            };
            URL fxmlLocation = null;
            for (String path : paths) {
                fxmlLocation = getClass().getResource(path);
                if (fxmlLocation != null) break;
            }
            if (fxmlLocation == null) {
                System.err.println("❌ Không tìm thấy FXML Admin!"); return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent adminRoot = loader.load();
            Stage stage = (Stage) btnAdminPanel.getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(adminRoot));
            stage.show();
        } catch (IOException e) {
            System.err.println("❌ Lỗi nạp Admin Layout.");
            e.printStackTrace();
        }
    }
}