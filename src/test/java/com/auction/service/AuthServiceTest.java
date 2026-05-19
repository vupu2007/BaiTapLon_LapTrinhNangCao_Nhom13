package com.auction.service;

import com.auction.shared.model.UserStore;
import com.auction.server.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

//Test cho RegisterService và LoginService.

@DisplayName("RegisterService & LoginService Tests")
class AuthServiceTest {

    private RegisterService registerService;
    private LoginService loginService;

    @BeforeEach
    void setUp() {
        // Reset UserStore trước mỗi test để tránh side effect
        UserStore.users.clear();
        registerService = new RegisterService();
        loginService    = new LoginService();
    }

    // ═══════════════════════════════════════════
    //  REGISTER SERVICE
    // ═══════════════════════════════════════════

    @Test
    @DisplayName("Đăng ký hợp lệ → trả về true và lưu vào UserStore")
    void testRegister_Success() {
        boolean result = registerService.register("alice", "pass123", "pass123");
        assertTrue(result);
        assertTrue(UserStore.users.containsKey("alice"));
    }

    @Test
    @DisplayName("Đăng ký thất bại khi username đã tồn tại")
    void testRegister_DuplicateUsername() {
        registerService.register("alice", "pass123", "pass123");
        boolean result = registerService.register("alice", "newpass", "newpass");
        assertFalse(result);
    }

    @Test
    @DisplayName("Đăng ký thất bại khi password và confirm không khớp")
    void testRegister_PasswordMismatch() {
        boolean result = registerService.register("bob", "pass123", "different");
        assertFalse(result);
        assertFalse(UserStore.users.containsKey("bob"));
    }

    @Test
    @DisplayName("Đăng ký thất bại khi username null")
    void testRegister_NullUsername() {
        boolean result = registerService.register(null, "pass123", "pass123");
        assertFalse(result);
    }

    @Test
    @DisplayName("Đăng ký thất bại khi password null")
    void testRegister_NullPassword() {
        boolean result = registerService.register("charlie", null, null);
        assertFalse(result);
    }

    @Test
    @DisplayName("Đăng ký thất bại khi username rỗng (sau trim)")
    void testRegister_BlankUsername() {
        boolean result = registerService.register("   ", "pass123", "pass123");
        assertFalse(result);
    }

    @Test
    @DisplayName("Đăng ký thất bại khi password rỗng")
    void testRegister_EmptyPassword() {
        boolean result = registerService.register("dave", "", "");
        assertFalse(result);
    }

    @Test
    @DisplayName("Đăng ký thành công: username có khoảng trắng đầu/cuối được trim")
    void testRegister_UsernameWithWhitespace() {
        boolean result = registerService.register("  eve  ", "pass123", "pass123");
        assertTrue(result);
        // Sau trim, key trong UserStore phải là "eve"
        assertTrue(UserStore.users.containsKey("eve"));
    }

    @Test
    @DisplayName("Đăng ký nhiều user khác nhau đều thành công")
    void testRegister_MultipleUsers() {
        assertTrue(registerService.register("user1", "pass1", "pass1"));
        assertTrue(registerService.register("user2", "pass2", "pass2"));
        assertEquals(2, UserStore.users.size());
    }

    // ═══════════════════════════════════════════
    //  LOGIN SERVICE
    // ═══════════════════════════════════════════

    @Test
    @DisplayName("Đăng nhập thành công với username và password đúng")
    void testLogin_Success() {
        UserStore.users.put("alice", "pass123");
        boolean result = loginService.login("alice", "pass123");
        assertTrue(result);
    }

    @Test
    @DisplayName("Đăng nhập thất bại với password sai")
    void testLogin_WrongPassword() {
        UserStore.users.put("alice", "pass123");
        boolean result = loginService.login("alice", "wrongpass");
        assertFalse(result);
    }

    @Test
    @DisplayName("Đăng nhập thất bại khi username không tồn tại")
    void testLogin_UserNotFound() {
        boolean result = loginService.login("unknown", "pass123");
        assertFalse(result);
    }

    @Test
    @DisplayName("Đăng nhập thất bại khi username null")
    void testLogin_NullUsername() {
        boolean result = loginService.login(null, "pass123");
        assertFalse(result);
    }

    @Test
    @DisplayName("Đăng nhập thất bại khi password null")
    void testLogin_NullPassword() {
        UserStore.users.put("alice", "pass123");
        boolean result = loginService.login("alice", null);
        assertFalse(result);
    }

    @Test
    @DisplayName("Đăng nhập thành công sau khi đăng ký (flow đầy đủ)")
    void testRegisterThenLogin_FullFlow() {
        registerService.register("frank", "mypass", "mypass");
        boolean loginResult = loginService.login("frank", "mypass");
        assertTrue(loginResult);
    }

    @Test
    @DisplayName("Đăng nhập thất bại sau khi đăng ký với password sai")
    void testRegisterThenLogin_WrongPassword() {
        registerService.register("grace", "correct", "correct");
        boolean loginResult = loginService.login("grace", "wrong");
        assertFalse(loginResult);
    }

    @Test
    @DisplayName("Login username với khoảng trắng được trim đúng")
    void testLogin_UsernameWithWhitespace() {
        UserStore.users.put("henry", "pass");
        boolean result = loginService.login("  henry  ", "pass");
        assertTrue(result);
    }
}
