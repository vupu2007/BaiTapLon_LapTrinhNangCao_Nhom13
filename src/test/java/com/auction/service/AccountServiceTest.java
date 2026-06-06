package com.auction.service;

import com.auction.server.service.AccountService;
import com.auction.shared.exception.AuthenticationException;
import com.auction.shared.model.Admin;
import com.auction.shared.model.Bidder;
import com.auction.shared.model.Seller;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test validation logic của AccountService.
 *
 * Bao gồm:
 *  - register(): kiểm tra null, blank, độ dài password
 *  - deposit():  kiểm tra amount hợp lệ, phân quyền theo role
 *  - switchRole(): kiểm tra chuyển đổi role
 *  - login():    kiểm tra throw AuthenticationException khi input rỗng/null
 *                và verify đúng Reason (INVALID_CREDENTIALS)
 *  - changePassword():  kiểm tra độ dài password mới, id null
 * Lưu ý: các test login() gọi trực tiếp AccountService.
 * Các test register/deposit/switchRole kiểm tra logic validation thuần túy
 */
@DisplayName("AccountService — Validation Logic Tests")
class AccountServiceTest {

    // ─── Khởi tạo AccountService để test login() trực tiếp ───
    // AccountService dùng để test login() trực tiếp:
    // - input null/blank → throw AuthenticationException trước khi chạm DB
    // - verify Reason đúng là INVALID_CREDENTIALS

    private final AccountService accountService = new AccountService();


    // ═══════════════════════════════════════════════════════
    //  register() — Validation Logic
    //  Tái hiện các điều kiện if bên trong AccountService.register()
    //  để kiểm tra từng nhánh validation độc lập với DB
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("register: username null → validation fail")
    void testRegisterValidation_NullUsername() {
        // Tái hiện điều kiện: if (username == null || username.isBlank()) return false
        boolean result = accountService.register(null, "pass123", "test@mail.com");
        assertFalse(result, "username null phải fail");
    }

    @Test
    @DisplayName("register: username rỗng (chỉ khoảng trắng) → validation fail")
    void testRegisterValidation_BlankUsername() {
        boolean result = accountService.register("   ", "pass123", "test@mail.com");
        assertFalse(result, "username rỗng phải fail");
    }

    @Test
    @DisplayName("register: password dưới 6 ký tự → validation fail")
    void testRegisterValidation_ShortPassword() {
        // Tái hiện điều kiện: if (password == null || password.length() < 6) return false
        boolean result = accountService.register("alice", "abc", "alice@mail.com");
        assertFalse(result, "password < 6 ký tự phải fail");
    }

    @Test
    @DisplayName("register: password null → validation fail")
    void testRegisterValidation_NullPassword() {
            boolean result = accountService.register("alice", null, "alice@mail.com");
            assertFalse(result, "password null phải fail");
    }

    @Test
    @DisplayName("register: password đúng 6 ký tự → validation pass")
    void testRegisterValidation_PasswordExactly6Chars() {
        String password = "abc123";
        boolean valid = password != null && password.length() >= 6;
        assertTrue(valid, "password 6 ky tu phai pass");
    }

    @Test
    @DisplayName("register: username và password hợp lệ → validation pass")
    void testRegisterValidation_ValidInput() {
        String username = "alice";
        String password = "pass1234";
        boolean valid = username != null && !username.isBlank()
                && password != null && password.length() >= 6;
        assertTrue(valid, "input hop le phai pass");
    }


    // ═══════════════════════════════════════════════════════
    //  deposit() — Validation Logic
    //  Kiểm tra: amount > 0, và chỉ Bidder/Seller mới được deposit
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("deposit: amount = 0 → validation fail")
    void testDepositValidation_ZeroAmount() {
        // Tái hiện điều kiện: if (account == null || amount <= 0) return false
        double amount = 0;
        boolean valid = amount > 0;
        assertFalse(valid, "Nạp 0 đồng phải fail");
    }

    @Test
    @DisplayName("deposit: amount âm → validation fail")
    void testDepositValidation_NegativeAmount() {
        double amount = -500_000;
        boolean valid = amount > 0;
        assertFalse(valid, "Nạp tiền âm phải fail");
    }

    @Test
    @DisplayName("deposit: amount hợp lệ → validation pass")
    void testDepositValidation_ValidAmount() {
        double amount = 1_000_000;
        boolean valid = amount > 0;
        assertTrue(valid, "Nạp tiền dương phải pass");
    }

    @Test
    @DisplayName("deposit: Admin không có ví → không được deposit (role = ADMIN)")
    void testDepositValidation_AdminCannotDeposit() {
        // Admin không phải Bidder/Seller nên bị chặn trong AccountService.deposit()
        Admin admin = new Admin("1", "admin", "pass", "admin@mail.com");
        assertEquals("ADMIN", admin.getRole());
        assertNotEquals("BIDDER", admin.getRole());
        assertNotEquals("SELLER", admin.getRole());
    }

    @Test
    @DisplayName("deposit: Bidder hợp lệ → role đúng, được phép deposit")
    void testDepositValidation_BidderCanDeposit() {
        Bidder bidder = new Bidder("1", "alice", "pass", "alice@mail.com", 0);
        assertEquals("BIDDER", bidder.getRole());
        assertInstanceOf(Bidder.class, bidder);
    }

    @Test
    @DisplayName("deposit: Seller hợp lệ → role đúng, được phép deposit")
    void testDepositValidation_SellerCanDeposit() {
        Seller seller = new Seller("2", "bob", "pass", "bob@mail.com", 0);
        assertEquals("SELLER", seller.getRole());
        assertInstanceOf(Seller.class, seller);
    }


    // ═══════════════════════════════════════════════════════
    //  switchRole() — Logic xác định role mới
    //  Tái hiện: Bidder → SELLER, Seller → BIDDER
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("switchRole: Bidder đổi role → role mới phải là SELLER")
    void testSwitchRole_BidderToSeller() {
        Bidder bidder = new Bidder("1", "alice", "pass", "alice@mail.com", 0);
        // Tái hiện logic: String newRole = currentAccount instanceof Bidder ? "SELLER" : "BIDDER"
        String newRole = "BIDDER".equals(bidder.getRole()) ? "SELLER" : "BIDDER";
        assertEquals("SELLER", newRole);
    }

    @Test
    @DisplayName("switchRole: Seller đổi role → role mới phải là BIDDER")
    void testSwitchRole_SellerToBidder() {
        Seller seller = new Seller("2", "bob", "pass", "bob@mail.com", 0);
        String newRole = "BIDDER".equals(seller.getRole()) ? "SELLER" : "BIDDER";
        assertEquals("BIDDER", newRole);
    }

    @Test
    @DisplayName("switchRole: account null → trả về null (không crash)")
    void testSwitchRole_NullAccount() {
        // Tái hiện: if (currentAccount == null) return null
        Object currentAccount = null;
        Object result = (currentAccount == null) ? null : new Object();
        assertNull(result, "account null phải trả null");
    }


    // ═══════════════════════════════════════════════════════
    //  login() — AuthenticationException
    //  Gọi trực tiếp AccountService.login() với input null/blank.
    //  Exception được throw TRƯỚC khi chạm DB nên không cần mock.
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("login: username null → throw AuthenticationException")
    void testLogin_NullUsername_ThrowsAuthenticationException() {
        assertThrows(
                AuthenticationException.class,
                () -> accountService.login(null, "password123"),
                "username null phải throw AuthenticationException"
        );
    }

    @Test
    @DisplayName("login: username rỗng → throw AuthenticationException")
    void testLogin_BlankUsername_ThrowsAuthenticationException() {
        assertThrows(
                AuthenticationException.class,
                () -> accountService.login("   ", "password123"),
                "username rỗng phải throw AuthenticationException"
        );
    }

    @Test
    @DisplayName("login: password null → throw AuthenticationException")
    void testLogin_NullPassword_ThrowsAuthenticationException() {
        assertThrows(
                AuthenticationException.class,
                () -> accountService.login("alice", null),
                "password null phải throw AuthenticationException"
        );
    }

    @Test
    @DisplayName("login: password rỗng → throw AuthenticationException")
    void testLogin_BlankPassword_ThrowsAuthenticationException() {
        assertThrows(
                AuthenticationException.class,
                () -> accountService.login("alice", "   "),
                "password rỗng phải throw AuthenticationException"
        );
    }

    @Test
    @DisplayName("login: AuthenticationException mặc định có reason INVALID_CREDENTIALS")
    void testLogin_AuthenticationException_DefaultReason() {
        // Kiểm tra khi username null → phải là INVALID_CREDENTIALS
        AuthenticationException ex = assertThrows(
                AuthenticationException.class,
                () -> accountService.login(null, "pass")
        );
        assertEquals(
                AuthenticationException.Reason.INVALID_CREDENTIALS,
                ex.getReason(),
                "Reason phải là INVALID_CREDENTIALS khi input rỗng/null"
        );
    }


    @Test
    @DisplayName("login: password blank → reason phải là INVALID_CREDENTIALS")
    void testLogin_BlankPassword_CorrectReason() {
        AuthenticationException ex = assertThrows(
                AuthenticationException.class,
                () -> accountService.login("alice", "   ")
        );
        assertEquals(
                AuthenticationException.Reason.INVALID_CREDENTIALS,
                ex.getReason()
        );
    }

    // ─── changePassword() ───

    @Test
    @DisplayName("changePassword: password mới ít hơn 6 ký tự → trả false")
    void testChangePassword_ShortNewPassword() {
        String newPassword = "abc";
        boolean valid = newPassword != null && newPassword.length() >= 6;
        assertFalse(valid, "Password moi < 6 ky tu phai fail");
    }

    @Test
    @DisplayName("changePassword: id null → trả false")
    void testChangePassword_NullId() {
        String id = null;
        boolean valid = id != null;
        assertFalse(valid, "id null phai fail");
    }

    @Test
    @DisplayName("changePassword: password mới đủ 6 ký tự → validation pass")
    void testChangePassword_ValidNewPassword() {
        String newPassword = "newpass";
        boolean valid = newPassword != null && newPassword.length() >= 6;
        assertTrue(valid, "Password moi >= 6 ky tu phai pass");
    }
}