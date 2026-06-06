package com.auction.service;

import com.auction.server.dao.AccountDAO;
import com.auction.server.service.AccountService;
import com.auction.shared.exception.AuthenticationException;
import com.auction.shared.model.Account;
import com.auction.shared.model.Admin;
import com.auction.shared.model.Bidder;
import com.auction.shared.model.Seller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test toàn diện cho AccountService.
 *
 * Chiến lược:
 *  - AccountService có constructor (AccountDAO) để inject mock → dùng @InjectMocks
 *  - Các method throw exception TRƯỚC khi gọi DAO (null/blank input) → test trực tiếp
 *    KHÔNG cần stub, vì code chưa chạm DAO
 *  - Các method cần DAO (login với sai pass, register check trùng username,
 *    walletTransaction, deposit, switchRole, changePassword...) → stub bằng when()
 *
 * Cấu trúc:
 *  1. login()              — input null/blank (không cần mock) + sai pass (cần mock)
 *  2. register()           — input validation + trùng username + thành công
 *  3. deposit()            — phân quyền role + amount + delegate walletTransaction
 *  4. walletTransaction()  — amount <= 0, account null, số dư thiếu, DEPOSIT/WITHDRAW
 *  5. switchRole()         — null account, Bidder→SELLER, Seller→BIDDER, DB fail
 *  6. changePassword()     — null/id, pass mới ngắn, sai pass cũ, DB fail, thành công
 *  7. updateProfile()      — null/blank username, thành công
 *  8. getUsernameById()    — id hợp lệ, account null, id không phải số
 *  9. getAllUsersAsMap()    — kiểm tra cấu trúc map trả về
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService — Tests Toàn Diện")
class AccountServiceTest {

    @Mock
    AccountDAO accountDAO;

    @InjectMocks
    AccountService accountService;

    // ── Dữ liệu dùng chung ────────────────────────────────────────────────────
    private Bidder bidder;
    private Seller seller;
    private Admin  admin;

    @BeforeEach
    void setUp() {
        bidder = new Bidder("5",  "alice", "pass123", "alice@mail.com", 2_000_000);
        seller = new Seller("10", "bob",   "pass123", "bob@mail.com",   5_000_000);
        admin  = new Admin("99",  "admin", "admin123", "admin@mail.com");
    }


    // ═══════════════════════════════════════════════════════
    //  login()
    //
    //  - Input null/blank → throw AuthenticationException TRƯỚC khi gọi DAO
    //    → KHÔNG stub accountDAO (nếu stub sẽ thừa; Mockito sẽ cảnh báo unnecessary stubbing)
    //  - Input hợp lệ nhưng sai pass → DAO trả null → throw AuthenticationException
    //  - Input hợp lệ, đúng pass → DAO trả Account → trả về Account
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("login: username null → throw AuthenticationException (không gọi DAO)")
    void testLogin_NullUsername_ThrowsException() {
        assertThrows(AuthenticationException.class,
                () -> accountService.login(null, "pass123"));
        verifyNoInteractions(accountDAO); // xác nhận DAO chưa bị gọi
    }

    @Test
    @DisplayName("login: username rỗng → throw AuthenticationException (không gọi DAO)")
    void testLogin_BlankUsername_ThrowsException() {
        assertThrows(AuthenticationException.class,
                () -> accountService.login("   ", "pass123"));
        verifyNoInteractions(accountDAO);
    }

    @Test
    @DisplayName("login: password null → throw AuthenticationException (không gọi DAO)")
    void testLogin_NullPassword_ThrowsException() {
        assertThrows(AuthenticationException.class,
                () -> accountService.login("alice", null));
        verifyNoInteractions(accountDAO);
    }

    @Test
    @DisplayName("login: password rỗng → throw AuthenticationException (không gọi DAO)")
    void testLogin_BlankPassword_ThrowsException() {
        assertThrows(AuthenticationException.class,
                () -> accountService.login("alice", "   "));
        verifyNoInteractions(accountDAO);
    }

    @Test
    @DisplayName("login: input null → exception có Reason = INVALID_CREDENTIALS")
    void testLogin_NullInput_ExceptionReason_InvalidCredentials() {
        AuthenticationException ex = assertThrows(AuthenticationException.class,
                () -> accountService.login(null, "pass123"));
        assertEquals(AuthenticationException.Reason.INVALID_CREDENTIALS, ex.getReason());
    }

    @Test
    @DisplayName("login: password rỗng → exception có Reason = INVALID_CREDENTIALS")
    void testLogin_BlankPassword_ExceptionReason_InvalidCredentials() {
        AuthenticationException ex = assertThrows(AuthenticationException.class,
                () -> accountService.login("alice", "   "));
        assertEquals(AuthenticationException.Reason.INVALID_CREDENTIALS, ex.getReason());
    }

    @Test
    @DisplayName("login: đúng định dạng nhưng DAO trả null (sai pass) → throw AuthenticationException")
    void testLogin_WrongPassword_DAOReturnsNull_ThrowsException() {
        when(accountDAO.login("alice", "wrongpass")).thenReturn(null);

        assertThrows(AuthenticationException.class,
                () -> accountService.login("alice", "wrongpass"));
    }

    @Test
    @DisplayName("login: DAO trả null → exception có Reason = INVALID_CREDENTIALS")
    void testLogin_DAOReturnsNull_ExceptionReason() {
        when(accountDAO.login("alice", "wrongpass")).thenReturn(null);

        AuthenticationException ex = assertThrows(AuthenticationException.class,
                () -> accountService.login("alice", "wrongpass"));

        assertEquals(AuthenticationException.Reason.INVALID_CREDENTIALS, ex.getReason());
    }

    @Test
    @DisplayName("login: đúng username + password → DAO trả Account, trả về Account đó")
    void testLogin_ValidCredentials_ReturnsAccount() throws AuthenticationException {
        when(accountDAO.login("alice", "pass123")).thenReturn(bidder);

        Account result = accountService.login("alice", "pass123");

        assertNotNull(result);
        assertEquals("alice", result.getUsername());
    }

    @Test
    @DisplayName("login: thành công → DAO.login() được gọi đúng 1 lần với đúng tham số")
    void testLogin_Success_DAOCalledOnce() throws AuthenticationException {
        when(accountDAO.login("alice", "pass123")).thenReturn(bidder);

        accountService.login("alice", "pass123");

        verify(accountDAO, times(1)).login("alice", "pass123");
    }


    // ═══════════════════════════════════════════════════════
    //  register()
    //
    //  Thứ tự kiểm tra trong service:
    //  1. username null/blank → false (không gọi DAO)
    //  2. password null / length < 6 → false (không gọi DAO)
    //  3. accountDAO.isUsernameExist() → true → false
    //  4. accountDAO.register() → kết quả từ DB
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("register: username null → false, không gọi DAO")
    void testRegister_NullUsername_ReturnsFalse() {
        boolean result = accountService.register(null, "pass123", "mail@test.com");
        assertFalse(result);
        verifyNoInteractions(accountDAO);
    }

    @Test
    @DisplayName("register: username rỗng → false, không gọi DAO")
    void testRegister_BlankUsername_ReturnsFalse() {
        boolean result = accountService.register("   ", "pass123", "mail@test.com");
        assertFalse(result);
        verifyNoInteractions(accountDAO);
    }

    @Test
    @DisplayName("register: password null → false, không gọi DAO")
    void testRegister_NullPassword_ReturnsFalse() {
        boolean result = accountService.register("alice", null, "mail@test.com");
        assertFalse(result);
        verifyNoInteractions(accountDAO);
    }

    @Test
    @DisplayName("register: password dưới 6 ký tự → false, không gọi DAO")
    void testRegister_ShortPassword_ReturnsFalse() {
        boolean result = accountService.register("alice", "abc", "mail@test.com");
        assertFalse(result);
        verifyNoInteractions(accountDAO);
    }

    @Test
    @DisplayName("register: password đúng 6 ký tự → qua được validation độ dài")
    void testRegister_PasswordExactly6Chars_PassesValidation() {
        // Stub tiếp theo: username chưa tồn tại, DB thành công
        when(accountDAO.isUsernameExist("alice")).thenReturn(false);
        when(accountDAO.register("alice", "abc123", "mail@test.com")).thenReturn(true);

        boolean result = accountService.register("alice", "abc123", "mail@test.com");
        assertTrue(result);
    }

    @Test
    @DisplayName("register: username đã tồn tại → false, không gọi accountDAO.register()")
    void testRegister_DuplicateUsername_ReturnsFalse() {
        when(accountDAO.isUsernameExist("alice")).thenReturn(true);

        boolean result = accountService.register("alice", "pass123", "mail@test.com");

        assertFalse(result);
        verify(accountDAO, never()).register(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("register: input hợp lệ, username chưa tồn tại, DB thành công → true")
    void testRegister_ValidInput_DBSuccess_ReturnsTrue() {
        when(accountDAO.isUsernameExist("newuser")).thenReturn(false);
        when(accountDAO.register("newuser", "pass123", "new@mail.com")).thenReturn(true);

        boolean result = accountService.register("newuser", "pass123", "new@mail.com");

        assertTrue(result);
        verify(accountDAO, times(1)).register("newuser", "pass123", "new@mail.com");
    }

    @Test
    @DisplayName("register: input hợp lệ nhưng DB thất bại → false")
    void testRegister_ValidInput_DBFail_ReturnsFalse() {
        when(accountDAO.isUsernameExist("newuser")).thenReturn(false);
        when(accountDAO.register("newuser", "pass123", "new@mail.com")).thenReturn(false);

        boolean result = accountService.register("newuser", "pass123", "new@mail.com");

        assertFalse(result);
    }


    // ═══════════════════════════════════════════════════════
    //  deposit()
    //
    //  Kiểm tra trong service:
    //  1. account null hoặc amount <= 0 → false
    //  2. account là Admin → false (Admin không có ví)
    //  3. account là Bidder/Seller + amount > 0 → delegate sang walletTransaction()
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("deposit: account null → false")
    void testDeposit_NullAccount_ReturnsFalse() {
        boolean result = accountService.deposit(null, 500_000);
        assertFalse(result);
    }

    @Test
    @DisplayName("deposit: amount = 0 → false")
    void testDeposit_ZeroAmount_ReturnsFalse() {
        boolean result = accountService.deposit(bidder, 0);
        assertFalse(result);
    }

    @Test
    @DisplayName("deposit: amount âm → false")
    void testDeposit_NegativeAmount_ReturnsFalse() {
        boolean result = accountService.deposit(bidder, -100_000);
        assertFalse(result);
    }

    @Test
    @DisplayName("deposit: Admin không có ví → false")
    void testDeposit_AdminAccount_ReturnsFalse() {
        boolean result = accountService.deposit(admin, 1_000_000);
        assertFalse(result);
        verifyNoInteractions(accountDAO); // Admin bị chặn trước khi gọi DAO
    }

    @Test
    @DisplayName("deposit: Bidder hợp lệ → gọi walletTransaction DEPOSIT, trả true khi DB thành công")
    void testDeposit_ValidBidder_CallsWalletTransaction() {
        // walletTransaction gọi: getAccountById → executeAtomicWalletUpdate → getAccountById → insertTransaction
        when(accountDAO.getAccountById(5)).thenReturn(bidder);
        when(accountDAO.executeAtomicWalletUpdate(5, 500_000, "DEPOSIT")).thenReturn(true);
        // lần gọi thứ 2 getAccountById (sau update) cũng cần stub
        Bidder updatedBidder = new Bidder("5", "alice", "pass123", "alice@mail.com", 2_500_000);
        when(accountDAO.getAccountById(5)).thenReturn(bidder).thenReturn(updatedBidder);

        boolean result = accountService.deposit(bidder, 500_000);

        assertTrue(result);
        verify(accountDAO, times(1)).executeAtomicWalletUpdate(5, 500_000, "DEPOSIT");
    }

    @Test
    @DisplayName("deposit: Seller hợp lệ → gọi walletTransaction DEPOSIT, trả true khi DB thành công")
    void testDeposit_ValidSeller_CallsWalletTransaction() {
        Seller updatedSeller = new Seller("10", "bob", "pass123", "bob@mail.com", 5_500_000);
        when(accountDAO.getAccountById(10)).thenReturn(seller).thenReturn(updatedSeller);
        when(accountDAO.executeAtomicWalletUpdate(10, 1_000_000, "DEPOSIT")).thenReturn(true);

        boolean result = accountService.deposit(seller, 1_000_000);

        assertTrue(result);
    }


    // ═══════════════════════════════════════════════════════
    //  walletTransaction()
    //
    //  Kiểm tra:
    //  1. amount <= 0 → false (không gọi DAO)
    //  2. getAccountById trả null → false
    //  3. WITHDRAW: số dư < amount → false
    //  4. WITHDRAW: số dư đủ → gọi executeAtomicWalletUpdate
    //  5. DEPOSIT: luôn cho qua kiểm tra số dư
    //  6. DB thất bại → false, không gọi insertTransaction
    //  7. DB thành công → gọi insertTransaction, trả true
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("walletTransaction: amount <= 0 → false, không gọi DAO")
    void testWalletTransaction_ZeroAmount_ReturnsFalse() {
        boolean result = accountService.walletTransaction(5, 0, "DEPOSIT");
        assertFalse(result);
        verifyNoInteractions(accountDAO);
    }

    @Test
    @DisplayName("walletTransaction: amount âm → false, không gọi DAO")
    void testWalletTransaction_NegativeAmount_ReturnsFalse() {
        boolean result = accountService.walletTransaction(5, -1, "DEPOSIT");
        assertFalse(result);
        verifyNoInteractions(accountDAO);
    }

    @Test
    @DisplayName("walletTransaction: getAccountById trả null → false")
    void testWalletTransaction_AccountNotFound_ReturnsFalse() {
        when(accountDAO.getAccountById(999)).thenReturn(null);

        boolean result = accountService.walletTransaction(999, 500_000, "DEPOSIT");

        assertFalse(result);
        verify(accountDAO, never()).executeAtomicWalletUpdate(anyInt(), anyDouble(), anyString());
    }

    @Test
    @DisplayName("walletTransaction WITHDRAW: số dư không đủ → false, không gọi executeAtomicWalletUpdate")
    void testWalletTransaction_Withdraw_InsufficientBalance_ReturnsFalse() {
        // bidder có 2tr, rút 3tr → fail
        when(accountDAO.getAccountById(5)).thenReturn(bidder);

        boolean result = accountService.walletTransaction(5, 3_000_000, "WITHDRAW");

        assertFalse(result);
        verify(accountDAO, never()).executeAtomicWalletUpdate(anyInt(), anyDouble(), anyString());
    }

    @Test
    @DisplayName("walletTransaction WITHDRAW: số dư bằng đúng amount → được phép rút")
    void testWalletTransaction_Withdraw_ExactBalance_Succeeds() {
        // bidder có 2tr, rút đúng 2tr → pass
        Bidder updatedBidder = new Bidder("5", "alice", "pass123", "alice@mail.com", 0);
        when(accountDAO.getAccountById(5)).thenReturn(bidder).thenReturn(updatedBidder);
        when(accountDAO.executeAtomicWalletUpdate(5, 2_000_000, "WITHDRAW")).thenReturn(true);

        boolean result = accountService.walletTransaction(5, 2_000_000, "WITHDRAW");

        assertTrue(result);
    }

    @Test
    @DisplayName("walletTransaction DEPOSIT: không kiểm tra số dư → luôn cho qua")
    void testWalletTransaction_Deposit_NoBalanceCheck() {
        // Bidder có 0 đồng vẫn có thể nhận deposit
        Bidder zeroBidder = new Bidder("5", "alice", "pass123", "alice@mail.com", 0);
        Bidder updatedBidder = new Bidder("5", "alice", "pass123", "alice@mail.com", 500_000);
        when(accountDAO.getAccountById(5)).thenReturn(zeroBidder).thenReturn(updatedBidder);
        when(accountDAO.executeAtomicWalletUpdate(5, 500_000, "DEPOSIT")).thenReturn(true);

        boolean result = accountService.walletTransaction(5, 500_000, "DEPOSIT");

        assertTrue(result);
    }

    @Test
    @DisplayName("walletTransaction: DB executeAtomicWalletUpdate thất bại → false, không gọi insertTransaction")
    void testWalletTransaction_DBFail_NoInsertTransaction() {
        when(accountDAO.getAccountById(5)).thenReturn(bidder);
        when(accountDAO.executeAtomicWalletUpdate(5, 500_000, "DEPOSIT")).thenReturn(false);

        boolean result = accountService.walletTransaction(5, 500_000, "DEPOSIT");

        assertFalse(result);
        verify(accountDAO, never()).insertTransaction(anyInt(), anyString(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("walletTransaction: DB thành công → gọi insertTransaction 1 lần, trả true")
    void testWalletTransaction_DBSuccess_CallsInsertTransaction() {
        Bidder updatedBidder = new Bidder("5", "alice", "pass123", "alice@mail.com", 2_500_000);
        when(accountDAO.getAccountById(5)).thenReturn(bidder).thenReturn(updatedBidder);
        when(accountDAO.executeAtomicWalletUpdate(5, 500_000, "DEPOSIT")).thenReturn(true);

        boolean result = accountService.walletTransaction(5, 500_000, "DEPOSIT");

        assertTrue(result);
        verify(accountDAO, times(1)).insertTransaction(eq(5), eq("DEPOSIT"), eq(500_000.0), anyDouble());
    }


    // ═══════════════════════════════════════════════════════
    //  switchRole()
    //
    //  Kiểm tra:
    //  1. account null → null
    //  2. Bidder → gọi DAO với newRole="SELLER"
    //  3. Seller → gọi DAO với newRole="BIDDER"
    //  4. DAO.switchRole() thất bại → null
    //  5. DAO thành công → trả Account mới từ getAccountById
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("switchRole: account null → trả null, không gọi DAO")
    void testSwitchRole_NullAccount_ReturnsNull() {
        Account result = accountService.switchRole(null);
        assertNull(result);
        verifyNoInteractions(accountDAO);
    }

    @Test
    @DisplayName("switchRole: Bidder → gọi DAO.switchRole với newRole='SELLER'")
    void testSwitchRole_Bidder_CallsDAOWithSellerRole() {
        when(accountDAO.switchRole(5, "SELLER")).thenReturn(true);
        Seller newSeller = new Seller("5", "alice", "pass123", "alice@mail.com", 2_000_000);
        when(accountDAO.getAccountById(5)).thenReturn(newSeller);

        accountService.switchRole(bidder);

        verify(accountDAO, times(1)).switchRole(5, "SELLER");
    }

    @Test
    @DisplayName("switchRole: Seller → gọi DAO.switchRole với newRole='BIDDER'")
    void testSwitchRole_Seller_CallsDAOWithBidderRole() {
        when(accountDAO.switchRole(10, "BIDDER")).thenReturn(true);
        Bidder newBidder = new Bidder("10", "bob", "pass123", "bob@mail.com", 5_000_000);
        when(accountDAO.getAccountById(10)).thenReturn(newBidder);

        accountService.switchRole(seller);

        verify(accountDAO, times(1)).switchRole(10, "BIDDER");
    }

    @Test
    @DisplayName("switchRole: DAO.switchRole thất bại → trả null")
    void testSwitchRole_DAOFail_ReturnsNull() {
        when(accountDAO.switchRole(5, "SELLER")).thenReturn(false);

        Account result = accountService.switchRole(bidder);

        assertNull(result);
        verify(accountDAO, never()).getAccountById(anyInt()); // không gọi getAccount khi fail
    }

    @Test
    @DisplayName("switchRole: DAO thành công → trả Account mới từ getAccountById")
    void testSwitchRole_DAOSuccess_ReturnsUpdatedAccount() {
        Seller newSeller = new Seller("5", "alice", "pass123", "alice@mail.com", 2_000_000);
        when(accountDAO.switchRole(5, "SELLER")).thenReturn(true);
        when(accountDAO.getAccountById(5)).thenReturn(newSeller);

        Account result = accountService.switchRole(bidder);

        assertNotNull(result);
        assertEquals("SELLER", result.getRole());
    }


    // ═══════════════════════════════════════════════════════
    //  changePassword()
    //
    //  Kiểm tra theo thứ tự trong service:
    //  1. id null, currentPassword null, newPassword null → false
    //  2. newPassword.length() < 6 → false (không gọi DAO)
    //  3. getAccountById trả null → false
    //  4. currentPassword không khớp → false
    //  5. Tất cả hợp lệ → gọi updatePasswordRaw, trả kết quả
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("changePassword: id null → false, không gọi DAO")
    void testChangePassword_NullId_ReturnsFalse() {
        boolean result = accountService.changePassword(null, "pass123", "newpass123");
        assertFalse(result);
        verifyNoInteractions(accountDAO);
    }

    @Test
    @DisplayName("changePassword: currentPassword null → false, không gọi DAO")
    void testChangePassword_NullCurrentPassword_ReturnsFalse() {
        boolean result = accountService.changePassword("5", null, "newpass123");
        assertFalse(result);
        verifyNoInteractions(accountDAO);
    }

    @Test
    @DisplayName("changePassword: newPassword null → false, không gọi DAO")
    void testChangePassword_NullNewPassword_ReturnsFalse() {
        boolean result = accountService.changePassword("5", "pass123", null);
        assertFalse(result);
        verifyNoInteractions(accountDAO);
    }

    @Test
    @DisplayName("changePassword: newPassword dưới 6 ký tự → false, không gọi DAO")
    void testChangePassword_ShortNewPassword_ReturnsFalse() {
        boolean result = accountService.changePassword("5", "pass123", "abc");
        assertFalse(result);
        verifyNoInteractions(accountDAO);
    }

    @Test
    @DisplayName("changePassword: getAccountById trả null → false")
    void testChangePassword_AccountNotFound_ReturnsFalse() {
        when(accountDAO.getAccountById(5)).thenReturn(null);

        boolean result = accountService.changePassword("5", "pass123", "newpass123");

        assertFalse(result);
        verify(accountDAO, never()).updatePasswordRaw(anyInt(), anyString());
    }

    @Test
    @DisplayName("changePassword: mật khẩu hiện tại không khớp → false")
    void testChangePassword_WrongCurrentPassword_ReturnsFalse() {
        // bidder.getPassword() = "pass123", nhưng truyền vào "wrongpass"
        when(accountDAO.getAccountById(5)).thenReturn(bidder);

        boolean result = accountService.changePassword("5", "wrongpass", "newpass123");

        assertFalse(result);
        verify(accountDAO, never()).updatePasswordRaw(anyInt(), anyString());
    }

    @Test
    @DisplayName("changePassword: mật khẩu đúng, newPassword >= 6 ký tự, DB thành công → true")
    void testChangePassword_ValidInput_DBSuccess_ReturnsTrue() {
        when(accountDAO.getAccountById(5)).thenReturn(bidder);
        when(accountDAO.updatePasswordRaw(5, "newpass123")).thenReturn(true);

        boolean result = accountService.changePassword("5", "pass123", "newpass123");

        assertTrue(result);
        verify(accountDAO, times(1)).updatePasswordRaw(5, "newpass123");
    }

    @Test
    @DisplayName("changePassword: DB updatePasswordRaw thất bại → false")
    void testChangePassword_DBFail_ReturnsFalse() {
        when(accountDAO.getAccountById(5)).thenReturn(bidder);
        when(accountDAO.updatePasswordRaw(5, "newpass123")).thenReturn(false);

        boolean result = accountService.changePassword("5", "pass123", "newpass123");

        assertFalse(result);
    }

    @Test
    @DisplayName("changePassword: đổi pass đúng 6 ký tự → qua được validation độ dài")
    void testChangePassword_NewPasswordExactly6Chars_PassesValidation() {
        when(accountDAO.getAccountById(5)).thenReturn(bidder);
        when(accountDAO.updatePasswordRaw(5, "abc123")).thenReturn(true);

        boolean result = accountService.changePassword("5", "pass123", "abc123");

        assertTrue(result);
    }


    // ═══════════════════════════════════════════════════════
    //  updateProfile()
    //
    //  Kiểm tra:
    //  1. id null → false, không gọi DAO
    //  2. newUsername null/blank → false, không gọi DAO
    //  3. Hợp lệ → gọi DAO.updateProfile, trả kết quả
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("updateProfile: id null → false, không gọi DAO")
    void testUpdateProfile_NullId_ReturnsFalse() {
        boolean result = accountService.updateProfile(null, "newname", "new@mail.com");
        assertFalse(result);
        verifyNoInteractions(accountDAO);
    }

    @Test
    @DisplayName("updateProfile: newUsername null → false, không gọi DAO")
    void testUpdateProfile_NullUsername_ReturnsFalse() {
        boolean result = accountService.updateProfile("5", null, "new@mail.com");
        assertFalse(result);
        verifyNoInteractions(accountDAO);
    }

    @Test
    @DisplayName("updateProfile: newUsername rỗng → false, không gọi DAO")
    void testUpdateProfile_BlankUsername_ReturnsFalse() {
        boolean result = accountService.updateProfile("5", "   ", "new@mail.com");
        assertFalse(result);
        verifyNoInteractions(accountDAO);
    }

    @Test
    @DisplayName("updateProfile: input hợp lệ, DB thành công → true")
    void testUpdateProfile_ValidInput_DBSuccess_ReturnsTrue() {
        when(accountDAO.updateProfile(5, "newname", "new@mail.com")).thenReturn(true);

        boolean result = accountService.updateProfile("5", "newname", "new@mail.com");

        assertTrue(result);
        verify(accountDAO, times(1)).updateProfile(5, "newname", "new@mail.com");
    }

    @Test
    @DisplayName("updateProfile: input hợp lệ, DB thất bại → false")
    void testUpdateProfile_ValidInput_DBFail_ReturnsFalse() {
        when(accountDAO.updateProfile(5, "newname", "new@mail.com")).thenReturn(false);

        boolean result = accountService.updateProfile("5", "newname", "new@mail.com");

        assertFalse(result);
    }


    // ═══════════════════════════════════════════════════════
    //  getUsernameById()
    //
    //  Đây là util method — không throw exception, luôn trả String.
    //  Kiểm tra: id hợp lệ, account null, id không phải số (NumberFormatException)
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("getUsernameById: id hợp lệ, account tồn tại → trả username đúng")
    void testGetUsernameById_ValidId_ReturnsUsername() {
        when(accountDAO.getAccountById(5)).thenReturn(bidder);

        String result = accountService.getUsernameById("5");

        assertEquals("alice", result);
    }

    @Test
    @DisplayName("getUsernameById: account không tồn tại trong DB → trả 'Ẩn danh'")
    void testGetUsernameById_AccountNotFound_ReturnsAnDanh() {
        when(accountDAO.getAccountById(999)).thenReturn(null);

        String result = accountService.getUsernameById("999");

        assertEquals("Ẩn danh", result);
    }

    @Test
    @DisplayName("getUsernameById: id không phải số → catch exception, trả 'Ẩn danh'")
    void testGetUsernameById_NonNumericId_ReturnsAnDanh() {
        // parseInt("abc") → NumberFormatException → catch → "Ẩn danh"
        String result = accountService.getUsernameById("abc");

        assertEquals("Ẩn danh", result);
        verifyNoInteractions(accountDAO); // exception trước khi gọi DAO
    }

    @Test
    @DisplayName("getUsernameById: id null → catch exception, trả 'Ẩn danh'")
    void testGetUsernameById_NullId_ReturnsAnDanh() {
        String result = accountService.getUsernameById(null);
        assertEquals("Ẩn danh", result);
    }


    // ═══════════════════════════════════════════════════════
    //  getAllUsersAsMap()
    //
    //  Kiểm tra cấu trúc Map trả về:
    //  - Có đúng 4 keys: id, username, role, balance, status
    //  - Bidder/Seller có balance đúng định dạng
    //  - Account bị khóa → status = "BANNED"; còn lại → "ACTIVE"
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("getAllUsersAsMap: trả về đúng số lượng user")
    void testGetAllUsersAsMap_ReturnsCorrectCount() {
        when(accountDAO.getAllAccounts()).thenReturn(List.of(bidder, seller, admin));

        List<Map<String, String>> result = accountService.getAllUsersAsMap();

        assertEquals(3, result.size());
    }

    @Test
    @DisplayName("getAllUsersAsMap: mỗi map có đầy đủ các key: id, username, role, balance, status")
    void testGetAllUsersAsMap_MapHasRequiredKeys() {
        when(accountDAO.getAllAccounts()).thenReturn(List.of(bidder));

        Map<String, String> map = accountService.getAllUsersAsMap().get(0);

        assertTrue(map.containsKey("id"));
        assertTrue(map.containsKey("username"));
        assertTrue(map.containsKey("role"));
        assertTrue(map.containsKey("balance"));
        assertTrue(map.containsKey("status"));
    }

    @Test
    @DisplayName("getAllUsersAsMap: Bidder chưa bị khóa → status = 'ACTIVE'")
    void testGetAllUsersAsMap_ActiveBidder_StatusActive() {
        // bidder.getIsLocked() = 0 (mặc định khi khởi tạo)
        when(accountDAO.getAllAccounts()).thenReturn(List.of(bidder));

        Map<String, String> map = accountService.getAllUsersAsMap().get(0);

        assertEquals("ACTIVE", map.get("status"));
    }

    @Test
    @DisplayName("getAllUsersAsMap: Account bị khóa (isLocked=1) → status = 'BANNED'")
    void testGetAllUsersAsMap_LockedAccount_StatusBanned() {
        Bidder lockedBidder = new Bidder("5", "alice", "pass123", "alice@mail.com", 0);
        lockedBidder.setIsLocked(1);
        when(accountDAO.getAllAccounts()).thenReturn(List.of(lockedBidder));

        Map<String, String> map = accountService.getAllUsersAsMap().get(0);

        assertEquals("BANNED", map.get("status"));
    }

    @Test
    @DisplayName("getAllUsersAsMap: Bidder có balance 2tr → format đúng '2.000.000 đ'")
    void testGetAllUsersAsMap_BidderBalance_CorrectFormat() {
        when(accountDAO.getAllAccounts()).thenReturn(List.of(bidder));

        Map<String, String> map = accountService.getAllUsersAsMap().get(0);

        assertEquals("2,000,000 đ", map.get("balance"));
    }

    @Test
    @DisplayName("getAllUsersAsMap: Admin không có balance → balance hiển thị '0 đ'")
    void testGetAllUsersAsMap_AdminBalance_Zero() {
        when(accountDAO.getAllAccounts()).thenReturn(List.of(admin));

        Map<String, String> map = accountService.getAllUsersAsMap().get(0);

        assertEquals("0 đ", map.get("balance"));
    }

    @Test
    @DisplayName("getAllUsersAsMap: DB trả danh sách rỗng → trả List rỗng")
    void testGetAllUsersAsMap_EmptyDB_ReturnsEmptyList() {
        when(accountDAO.getAllAccounts()).thenReturn(List.of());

        List<Map<String, String>> result = accountService.getAllUsersAsMap();

        assertTrue(result.isEmpty());
    }
}