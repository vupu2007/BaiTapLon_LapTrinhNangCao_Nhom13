package com.auction.service;

import com.auction.shared.exception.AuthenticationException;
import com.auction.shared.model.UserStore;
import com.auction.server.service.AccountService;
import com.auction.server.service.RegisterService;
import com.auction.server.service.LoginService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cho RegisterService và LoginService.
 *
 * RegisterService.register(username, password, email):
 *  - Kiểm tra null, blank, email hợp lệ (@), username trùng
 *
 * LoginService.login(username, password):
 *  - Kiểm tra đăng nhập đúng/sai với UserStore in-memory
 *
 * AccountService.login(username, password):
 *  - Kiểm tra throw AuthenticationException khi input null/blank
 */
@DisplayName("AuthService Tests — RegisterService, LoginService, AccountService")
class AuthServiceTest {

    private RegisterService registerService;
    private LoginService loginService;

    // AccountService để test AuthenticationException
    private final AccountService accountService = new AccountService();

    @BeforeEach
    void setUp() {
        // Reset UserStore trước mỗi test để tránh side effect giữa các test
        UserStore.users.clear();
        registerService = new RegisterService();
        loginService    = new LoginService();
    }


    // ═══════════════════════════════════════════════════════
    //  REGISTER SERVICE
    //  register(username, password, email)
    //  Kiểm tra: null check, blank check, email hợp lệ, username trùng
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("register: hợp lệ → trả true và lưu vào UserStore")
    void testRegister_Success() {
        boolean result = registerService.register("alice", "pass123", "alice@mail.com");
        assertTrue(result, "Dữ liệu hợp lệ phải đăng ký thành công");
        assertTrue(UserStore.users.containsKey("alice"), "alice phải có trong UserStore");
    }

    @Test
    @DisplayName("register: username đã tồn tại → trả false")
    void testRegister_DuplicateUsername() {
        // Đăng ký lần 1 thành công
        registerService.register("alice", "pass123", "alice@mail.com");
        // Đăng ký lần 2 cùng username → phải fail
        boolean result = registerService.register("alice", "newpass", "alice2@mail.com");
        assertFalse(result, "Username trùng phải fail");
    }

    @Test
    @DisplayName("register: email không có @ → trả false")
    void testRegister_InvalidEmail_NoAtSign() {
        // RegisterService kiểm tra email.contains("@")
        boolean result = registerService.register("bob", "pass123", "bobmail.com");
        assertFalse(result, "Email không có @ phải fail");
        assertFalse(UserStore.users.containsKey("bob"), "bob không được lưu vào UserStore");
    }

    @Test
    @DisplayName("register: email hợp lệ có @ → trả true")
    void testRegister_ValidEmail() {
        boolean result = registerService.register("carol", "pass123", "carol@test.com");
        assertTrue(result, "Email có @ phải pass");
    }

    @Test
    @DisplayName("register: username null → trả false")
    void testRegister_NullUsername() {
        boolean result = registerService.register(null, "pass123", "test@mail.com");
        assertFalse(result, "username null phải fail");
    }

    @Test
    @DisplayName("register: password null → trả false")
    void testRegister_NullPassword() {
        boolean result = registerService.register("dave", null, "dave@mail.com");
        assertFalse(result, "password null phải fail");
    }

    @Test
    @DisplayName("register: email null → trả false")
    void testRegister_NullEmail() {
        boolean result = registerService.register("eve", "pass123", null);
        assertFalse(result, "email null phải fail");
    }

    @Test
    @DisplayName("register: username rỗng sau trim → trả false")
    void testRegister_BlankUsername() {
        // RegisterService trim username trước khi check isEmpty()
        boolean result = registerService.register("   ", "pass123", "test@mail.com");
        assertFalse(result, "username chỉ khoảng trắng phải fail");
    }

    @Test
    @DisplayName("register: password rỗng → trả false")
    void testRegister_EmptyPassword() {
        boolean result = registerService.register("frank", "", "frank@mail.com");
        assertFalse(result, "password rỗng phải fail");
    }

    @Test
    @DisplayName("register: username có khoảng trắng đầu/cuối → được trim, đăng ký thành công")
    void testRegister_UsernameWithWhitespace() {
        // RegisterService trim username trước khi lưu
        boolean result = registerService.register("  grace  ", "pass123", "grace@mail.com");
        assertTrue(result, "username có khoảng trắng phải được trim và đăng ký thành công");
        // Sau trim key trong UserStore phải là "grace"
        assertTrue(UserStore.users.containsKey("grace"), "UserStore phải chứa key 'grace' sau trim");
    }

    @Test
    @DisplayName("register: nhiều user khác nhau → đều thành công")
    void testRegister_MultipleUsers() {
        assertTrue(registerService.register("user1", "pass1", "user1@mail.com"));
        assertTrue(registerService.register("user2", "pass2", "user2@mail.com"));
        assertEquals(2, UserStore.users.size(), "UserStore phải chứa đúng 2 user");
    }


    // ═══════════════════════════════════════════════════════
    //  LOGIN SERVICE
    //  login(username, password) — dùng UserStore in-memory
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("login: username và password đúng → trả true")
    void testLogin_Success() {
        UserStore.users.put("alice", "pass123");
        boolean result = loginService.login("alice", "pass123");
        assertTrue(result, "Đăng nhập đúng phải trả true");
    }

    @Test
    @DisplayName("login: password sai → trả false")
    void testLogin_WrongPassword() {
        UserStore.users.put("alice", "pass123");
        boolean result = loginService.login("alice", "wrongpass");
        assertFalse(result, "Password sai phải trả false");
    }

    @Test
    @DisplayName("login: username không tồn tại → trả false")
    void testLogin_UserNotFound() {
        boolean result = loginService.login("unknown", "pass123");
        assertFalse(result, "Username không tồn tại phải trả false");
    }

    @Test
    @DisplayName("login: username null → trả false")
    void testLogin_NullUsername() {
        // LoginService kiểm tra: if (user == null || pass == null) return false
        boolean result = loginService.login(null, "pass123");
        assertFalse(result, "username null phải trả false");
    }

    @Test
    @DisplayName("login: password null → trả false")
    void testLogin_NullPassword() {
        UserStore.users.put("alice", "pass123");
        boolean result = loginService.login("alice", null);
        assertFalse(result, "password null phải trả false");
    }

    @Test
    @DisplayName("login: username có khoảng trắng được trim đúng → trả true")
    void testLogin_UsernameWithWhitespace() {
        // LoginService trim username trước khi tìm trong UserStore
        UserStore.users.put("henry", "pass");
        boolean result = loginService.login("  henry  ", "pass");
        assertTrue(result, "username có khoảng trắng phải được trim và đăng nhập thành công");
    }

    @Test
    @DisplayName("flow đầy đủ: register rồi login → thành công")
    void testRegisterThenLogin_FullFlow() {
        registerService.register("frank", "mypass", "frank@mail.com");
        boolean loginResult = loginService.login("frank", "mypass");
        assertTrue(loginResult, "Đăng nhập sau đăng ký phải thành công");
    }

    @Test
    @DisplayName("flow đầy đủ: register rồi login sai password → thất bại")
    void testRegisterThenLogin_WrongPassword() {
        registerService.register("grace", "correct", "grace@mail.com");
        boolean loginResult = loginService.login("grace", "wrong");
        assertFalse(loginResult, "Đăng nhập sai password phải thất bại");
    }


    // ═══════════════════════════════════════════════════════
    //  ACCOUNT SERVICE — AuthenticationException
    //  Gọi trực tiếp AccountService.login() với input null/blank.
    //  Exception throw TRƯỚC khi chạm DB nên không cần mock.
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("AccountService.login: username null → throw AuthenticationException")
    void testAccountLogin_NullUsername_ThrowsException() {
        assertThrows(
                AuthenticationException.class,
                () -> accountService.login(null, "password123"),
                "username null phải throw AuthenticationException"
        );
    }

    @Test
    @DisplayName("AccountService.login: username rỗng → throw AuthenticationException")
    void testAccountLogin_BlankUsername_ThrowsException() {
        assertThrows(
                AuthenticationException.class,
                () -> accountService.login("   ", "password123"),
                "username rỗng phải throw AuthenticationException"
        );
    }

    @Test
    @DisplayName("AccountService.login: password null → throw AuthenticationException")
    void testAccountLogin_NullPassword_ThrowsException() {
        assertThrows(
                AuthenticationException.class,
                () -> accountService.login("alice", null),
                "password null phải throw AuthenticationException"
        );
    }

    @Test
    @DisplayName("AccountService.login: reason phải là INVALID_CREDENTIALS khi input rỗng")
    void testAccountLogin_ExceptionReason_IsInvalidCredentials() {
        AuthenticationException ex = assertThrows(
                AuthenticationException.class,
                () -> accountService.login(null, "pass")
        );
        assertEquals(
                AuthenticationException.Reason.INVALID_CREDENTIALS,
                ex.getReason(),
                "Reason phải là INVALID_CREDENTIALS khi input null/blank"
        );
    }
}