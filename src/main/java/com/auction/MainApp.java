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
            // Sửa đường dẫn trỏ thẳng vào màn hình Đăng nhập
            URL fxmlLocation = getClass().getResource("/view/LoginView.fxml");

            if (fxmlLocation == null) {
                System.err.println("LỖI: Không tìm thấy file LoginView.fxml!");
                System.err.println("Hãy kiểm tra lại xem file đã nằm đúng trong thư mục: src/main/resources/view/LoginView.fxml chưa nhé.");
                return;
            }

            Parent root = FXMLLoader.load(fxmlLocation);
            Scene scene = new Scene(root);

            primaryStage.setTitle("Đăng nhập - Hệ thống đấu giá trực tuyến");
            primaryStage.setScene(scene);

            // Tắt phóng to toàn màn hình và khóa resize để form đăng nhập không bị vỡ layout
            primaryStage.setResizable(false);

            // Hoặc nếu bạn muốn đặt kích thước cố định cho cửa sổ Login thì dùng:
            // primaryStage.setWidth(600);
            // primaryStage.setHeight(400);

            primaryStage.show();
        }
    }

    public static void main(String[] args) {
        Application.launch(AppUI.class, args);
    }
}