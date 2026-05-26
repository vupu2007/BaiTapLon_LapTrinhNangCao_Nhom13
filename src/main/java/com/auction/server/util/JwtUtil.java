package com.auction.server.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import com.auction.shared.model.User;
import java.security.Key;
import java.util.Base64;
import java.util.Date;

public class JwtUtil {

    private static final Key KEY;
    private static final long EXPIRATION_TIME = 86400000; // 24 giờ

    // Khối static này tự động chạy DUY NHẤT 1 LẦN khi Server khởi động để cấu hình chìa khóa
    static {
        // 1. Đọc chuỗi bí mật từ hệ điều hành / môi trường máy chủ
        String secret = System.getenv("JWT_SECRET_KEY");

        // 2. Phương án dự phòng (Fallback): Nếu chưa cấu hình biến môi trường thì tự dùng chuỗi này để không bị sập app khi dev ở máy cá nhân
        if (secret == null || secret.isBlank()) {
            secret = "Default_Fallback_Secret_Key_For_Local_Development_Only_Nhom13_2026";
            System.out.println("⚠️ [JWT-WARNING] Chưa cấu hình 'JWT_SECRET_KEY' trong Environment Variables. Đang dùng khóa dự phòng!");
        }

        // 3. Sử dụng Base64 thuần của JDK để mã hóa chuỗi byte (Đúng chuẩn dự án lớn)
        byte[] keyBytes = Base64.getEncoder().encode(secret.getBytes());
        KEY = Keys.hmacShaKeyFor(keyBytes);
    }

    // Hàm dùng để tạo Token khi User đăng nhập thành công
    public static String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getUsername()) // Lưu username vào thẻ
                .claim("role", user.getRole())  // Lưu vai trò (Admin/Bidder)
                .setIssuedAt(new Date())        // Ngày cấp
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // Ngày hết hạn
                .signWith(KEY, SignatureAlgorithm.HS256) // Ký tên bằng chìa khóa bảo mật
                .compact();
    }

    // Hàm dùng để kiểm tra một Token gửi lên có hợp lệ không
    public static String validateToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject(); // Nếu đúng thì trả về Username
        } catch (Exception e) {
            System.err.println("⚠️ Thẻ Token giả mạo hoặc đã hết hạn: " + e.getMessage());
            return null;
        }
    }
}