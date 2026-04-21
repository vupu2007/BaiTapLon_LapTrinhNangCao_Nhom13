package com.auction.controller;

import com.auction.model.User;
import com.auction.model.Bidder;
// import com.auction.dao.UserDAO; // Bạn sẽ cần tạo class này sau

public class UserController {

    // Khởi tạo DAO (Nơi chứa các câu lệnh SQL thực tế)
    // private UserDAO userDAO = new UserDAO();

    /**
     * Xử lý logic đăng nhập
     */
    public User loginUser(String username, String password) {
        System.out.println("Server đang xử lý đăng nhập cho: " + username);

        // Đoạn này trong thực tế sẽ gọi database:
        // User currentUser = userDAO.getUserByUsername(username);

        // Mock (Giả lập) dữ liệu để test tạm thời:
        if (username.equals("testuser") && password.equals("123456")) {
            // Trả về một đối tượng Bidder nếu đăng nhập đúng
            return new Bidder("U001", username, password);
        }

        System.out.println("Sai tên đăng nhập hoặc mật khẩu!");
        return null; // Đăng nhập thất bại
    }

    /**
     * Xử lý logic đăng ký
     */
    public boolean registerUser(String username, String password, String role) {
        // Kiểm tra xem username đã tồn tại chưa
        // boolean exists = userDAO.checkUsernameExists(username);
        // if (exists) return false;

        // Mã hóa password nếu cần
        // Thực hiện lưu vào Database thông qua DAO
        // userDAO.saveUser(newUser);

        System.out.println("Đã đăng ký thành công user: " + username + " với vai trò " + role);
        return true;
    }
}