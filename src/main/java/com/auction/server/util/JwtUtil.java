package com.auction.server.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import com.auction.shared.model.User;
import java.security.Key;
import java.util.Date;

public class JwtUtil {
    // Tạo một chìa khóa bí mật (Secret Key) để ký tên lên thẻ
    private static final Key KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    // Thời gian sống của thẻ (Token): 24 giờ
    private static final long EXPIRATION_TIME = 86400000;

    // Hàm dùng để tạo Token khi User đăng nhập thành công
    public static String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getUsername()) // Lưu username vào thẻ
                .claim("role", user.getRole())  // Lưu vai trò (Admin/Bidder)
                .setIssuedAt(new Date())        // Ngày cấp
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // Ngày hết hạn
                .signWith(KEY)                  // Ký tên bằng chìa khóa bí mật
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
            return null; // Nếu thẻ giả hoặc hết hạn thì trả về null
        }
    }
}