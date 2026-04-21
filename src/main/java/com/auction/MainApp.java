package com.auction;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.net.URL;

public class MainApp {

    public static class AppUI extends Application {
        @Override
        public void start(Stage primaryStage) throws Exception {
            // Đường dẫn đến file giao diện trong thư mục resources
            URL fxmlLocation = getClass().getResource("/com.auction.view/LoginView.fxml");

            if (fxmlLocation == null) {
                System.err.println("LỖI: Không tìm thấy file LoginView.fxml! Hãy kiểm tra lại thư mục resources.");
                return;
            }

            // Load giao diện
            Parent root = FXMLLoader.load(fxmlLocation);
            Scene scene = new Scene(root);

            primaryStage.setTitle("Hệ thống đấu giá trực tuyến - Nhóm 13");
            primaryStage.setScene(scene);

            // Lệnh giúp cửa sổ tự động to toàn màn hình khi mở
            primaryStage.setMaximized(true);

            primaryStage.show();
        }
    }

    public static void main(String[] args) {
        Application.launch(AppUI.class, args);
    }
}