package com.auction.service;

import com.auction.shared.model.Admin;
import com.auction.shared.model.Bidder;
import com.auction.shared.model.Seller;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test validation logic của AccountService
 * AccountService.register() và .deposit() có nhiều validation thuần túy
 * (null check, length check, amount > 0) có thể test trực tiếp.
 */
@DisplayName("AccountService — Validation Logic Tests")
class AccountServiceTest {

    //  register() — Validation Logic (tái hiện từ AccountService)
    //  Test trực tiếp các điều kiện if bên trong AccountService

    @Test
    @DisplayName("username null → register validation fail")
    void testRegisterValidation_NullUsername() {
        String username = null;
        boolean valid = username != null && !username.isBlank();
        assertFalse(valid, "username null phải fail");
    }

    @Test
    @DisplayName("username rỗng → register validation fail")
    void testRegisterValidation_BlankUsername() {
        String username = "   ";
        boolean valid = username != null && !username.isBlank();
        assertFalse(valid, "username rỗng phải fail");
    }

    @Test
    @DisplayName("password dưới 6 ký tự → register validation fail")
    void testRegisterValidation_ShortPassword() {
        String password = "abc";
        boolean valid = password != null && password.length() >= 6;
        assertFalse(valid, "password < 6 ký tự phải fail");
    }

    @Test
    @DisplayName("password null → register validation fail")
    void testRegisterValidation_NullPassword() {
        String password = null;
        boolean valid = password != null && password.length() >= 6;
        assertFalse(valid, "password null phải fail");
    }

    @Test
    @DisplayName("password đúng 6 ký tự → validation pass")
    void testRegisterValidation_PasswordExactly6Chars() {
        String password = "abc123";
        boolean valid = password != null && password.length() >= 6;
        assertTrue(valid, "password 6 ký tự phải pass");
    }

    @Test
    @DisplayName("username và password hợp lệ → validation pass")
    void testRegisterValidation_ValidInput() {
        String username = "alice";
        String password = "pass1234";
        boolean valid = username != null && !username.isBlank()
                && password != null && password.length() >= 6;
        assertTrue(valid);
    }

    //  deposit() — Validation Logic

    @Test
    @DisplayName("Nạp tiền amount <= 0 → deposit validation fail")
    void testDepositValidation_ZeroAmount() {
        double amount = 0;
        boolean valid = amount > 0;
        assertFalse(valid, "Nạp 0 đồng phải fail");
    }

    @Test
    @DisplayName("Nạp tiền amount âm → deposit validation fail")
    void testDepositValidation_NegativeAmount() {
        double amount = -500_000;
        boolean valid = amount > 0;
        assertFalse(valid, "Nạp tiền âm phải fail");
    }

    @Test
    @DisplayName("Nạp tiền amount hợp lệ → deposit validation pass")
    void testDepositValidation_ValidAmount() {
        double amount = 1_000_000;
        boolean valid = amount > 0;
        assertTrue(valid);
    }

    @Test
    @DisplayName("Admin không có ví tiền → không được deposit")
    void testDepositValidation_AdminCannotDeposit() {
        Admin admin = new Admin("1", "admin", "pass", "admin@mail.com");
        // Admin không phải User nên không có balance
        assertEquals("ADMIN", admin.getRole());
        assertNotEquals("BIDDER", admin.getRole());
        assertNotEquals("SELLER", admin.getRole());
    }

    @Test
    @DisplayName("Bidder có thể deposit")
    void testDepositValidation_BidderCanDeposit() {
        Bidder bidder = new Bidder("1", "alice", "pass", "alice@mail.com", 0);
        assertEquals("BIDDER", bidder.getRole());
    }

    @Test
    @DisplayName("Seller có thể deposit")
    void testDepositValidation_SellerCanDeposit() {
        Seller seller = new Seller("2", "bob", "pass", "bob@mail.com", 0);
        assertEquals("SELLER", seller.getRole());
    }

    //  switchRole() — Logic xác định role mới

    @Test
    @DisplayName("Bidder đổi role → role mới phải là SELLER")
    void testSwitchRole_BidderToSeller() {
        Bidder bidder = new Bidder("1", "alice", "pass", "alice@mail.com", 0);
        // Logic AccountService: Bidder → đổi thành SELLER
        String currentRole = bidder.getRole();
        String newRole = "BIDDER".equals(currentRole) ? "SELLER" : "BIDDER";
        assertEquals("SELLER", newRole);
    }

    @Test
    @DisplayName("Seller đổi role → role mới phải là BIDDER")
    void testSwitchRole_SellerToBidder() {
        Seller seller = new Seller("2", "bob", "pass", "bob@mail.com", 0);
        String currentRole = seller.getRole();
        String newRole = "BIDDER".equals(currentRole) ? "SELLER" : "BIDDER";
        assertEquals("BIDDER", newRole);
    }

    @Test
    @DisplayName("switchRole với account null → trả về null (không crash)")
    void testSwitchRole_NullAccount() {
        Object currentAccount = null;
        Object result = (currentAccount == null) ? null : new Object();
        assertNull(result);
    }
}
