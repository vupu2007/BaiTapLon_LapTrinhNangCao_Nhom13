package com.auction.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class SwitcherController {
    // 1. Chỉ khai báo MỘT LẦN duy nhất các biến này
    @FXML private VBox roleBox;        // Khung Sidebar (chứa chữ Tư cách hiện tại)
    @FXML private VBox buyerMenu;      // Nhóm nút cho người mua
    @FXML private VBox sellerMenu;     // Nhóm nút cho người bán
    @FXML private Label lblRoleSidebar; // Nhãn "Người mua/bán"
    @FXML private Label lblRoleTitle;   // Nhãn nhỏ "Tư cách hiện tại"

    /**
     * Hàm xử lý logic chuyển đổi giao diện Sidebar
     */
    public void updateRoleUI(boolean isBuyer) {
        if (isBuyer) {
            // Chế độ NGƯỜI MUA (Màu hồng)
            roleBox.setStyle("-fx-background-color: #fae8ff; -fx-background-radius: 15; -fx-padding: 20;");
            lblRoleSidebar.setText("🛒 Người mua");
            lblRoleSidebar.setStyle("-fx-text-fill: #86198f; -fx-font-weight: bold; -fx-font-size: 18;");
            lblRoleTitle.setStyle("-fx-text-fill: #86198f; -fx-font-size: 11;");

            buyerMenu.setVisible(true);
            buyerMenu.setManaged(true);
            sellerMenu.setVisible(false);
            sellerMenu.setManaged(false);
        } else {
            // Chế độ NGƯỜI BÁN (Màu xanh)
            roleBox.setStyle("-fx-background-color: #e0f2fe; -fx-background-radius: 15; -fx-padding: 20;");
            lblRoleSidebar.setText("🏪 Người bán");
            lblRoleSidebar.setStyle("-fx-text-fill: #0369a1; -fx-font-weight: bold; -fx-font-size: 18;");
            lblRoleTitle.setStyle("-fx-text-fill: #0369a1; -fx-font-size: 11;");

            buyerMenu.setVisible(false);
            buyerMenu.setManaged(false);
            sellerMenu.setVisible(true);
            sellerMenu.setManaged(true);
        }
    }
}