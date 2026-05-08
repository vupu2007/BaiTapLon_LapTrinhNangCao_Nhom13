package com.auction.controller;

import com.auction.model.Account;
import com.auction.model.Auction;
import com.auction.model.User;
import com.auction.service.AccountService;
import com.auction.service.AuctionService;
import com.auction.util.CurrentUser;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.List;

public class MainController {

    @FXML private Label welcomeLabel;
    @FXML private Label balanceLabel;
    @FXML private Label ongoingLabel;
    @FXML private Label wonLabel;
    @FXML private VBox hotItemsContainer;

    private final AuctionService auctionService = new AuctionService();
    private final AccountService accountService = new AccountService();

    @FXML
    public void initialize() {
        Account current = CurrentUser.getUser();
        if (current == null) return;

        welcomeLabel.setText("Chào mừng, " + current.getUsername() + "!");

        // Hiện balance nếu là User (Bidder/Seller)
        if (current instanceof User) {
            balanceLabel.setText(String.format("%.0f VNĐ", ((User) current).getBalance()));
        } else {
            balanceLabel.setText("N/A");
        }

        loadActiveAuctions();
    }

    private void loadActiveAuctions() {
        hotItemsContainer.getChildren().clear();

        List<Auction> auctions = auctionService.getActiveAuctions();
        ongoingLabel.setText(String.valueOf(auctions.size()));

        for (Auction auction : auctions) {
            VBox card = createAuctionCard(auction);
            hotItemsContainer.getChildren().add(card);
        }
    }

    private VBox createAuctionCard(Auction auction) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-border-color: #ddd; -fx-border-radius: 8;");

        Label itemId = new Label("Sản phẩm: " + auction.getItemId());
        Label price  = new Label("Giá hiện tại: " + String.format("%.0f", auction.getCurrentPrice()) + " VNĐ");
        Label endTime = new Label("Kết thúc: " + auction.getEndTime());

        Button bidButton = new Button("Đấu giá ngay");
        bidButton.setOnAction(e -> goToAuction(auction.getId()));

        card.getChildren().addAll(itemId, price, endTime, bidButton);
        return card;
    }

    private void goToAuction(int auctionId) {
        // TODO: truyền auctionId sang AuctionController
        System.out.println("Mở phiên đấu giá: " + auctionId);
    }

    @FXML
    public void handleSwitchRole(ActionEvent event) {
        Account current = CurrentUser.getUser();
        Account updated = accountService.switchRole(current);

        if (updated != null) {
            CurrentUser.setUser(updated);
            showAlert(Alert.AlertType.INFORMATION, "Thành công",
                    "Đã chuyển sang vai trò: " + updated.getRole());
            initialize(); // Refresh dashboard
        } else {
            showAlert(Alert.AlertType.ERROR, "Thất bại", "Không thể đổi vai trò!");
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        CurrentUser.logOut();
        switchScene(event, "/view/LoginView.fxml", "Đăng nhập");
    }

    private void switchScene(ActionEvent event, String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle(title);
        } catch (IOException e) {
            System.err.println("Không tìm thấy file: " + fxmlPath);
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}