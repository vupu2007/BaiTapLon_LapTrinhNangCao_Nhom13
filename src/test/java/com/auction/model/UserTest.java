package com.auction.model;

import com.auction.shared.model.Account;
import com.auction.shared.model.Admin;
import com.auction.shared.model.Bidder;
import com.auction.shared.model.Seller;
import com.auction.shared.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class cho cây kế thừa User: Bidder, Seller, Admin.
 * Bao gồm: constructor, role, balance, displayRole(), đa hình,
 * totalDeposit/totalWithdraw (Bidder và Seller).
 *
 * Cây kế thừa:
 *  Account (abstract)
 *    ├── User (abstract) → Bidder, Seller
 *    └── Admin
 */
@DisplayName("User Hierarchy Tests (Bidder / Seller / Admin)")
class UserTest {

    private Bidder bidder;
    private Seller seller;
    private Admin  admin;

    @BeforeEach
    void setUp() {
        bidder = new Bidder("1", "alice", "pass123", "alice@mail.com", 2_000_000);
        seller = new Seller("2", "bob",   "pass456", "bob@mail.com",   5_000_000);
        admin  = new Admin("3",  "admin", "adminpass", "admin@mail.com");
    }


    // ═══════════════════════════════════════════════════════
    //  Bidder
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("Bidder: constructor khởi tạo đúng tất cả field")
    void testBidderConstructor() {
        assertEquals("1",           bidder.getId());
        assertEquals("alice",       bidder.getUsername());
        assertEquals("BIDDER",      bidder.getRole());
        assertEquals("alice@mail.com", bidder.getEmail());
        assertEquals(2_000_000,     bidder.getBalance());
    }

    @Test
    @DisplayName("Bidder.displayRole() trả về 'Bidder (Buyer)'")
    void testBidderDisplayRole() {
        assertEquals("Bidder (Buyer)", bidder.displayRole());
    }

    @Test
    @DisplayName("Bidder.setBalance() cập nhật số dư đúng")
    void testBidderSetBalance() {
        bidder.setBalance(1_500_000);
        assertEquals(1_500_000, bidder.getBalance());
    }

    @Test
    @DisplayName("Bidder: totalDeposit và totalWithdraw mặc định = 0")
    void testBidderTotalDepositWithdraw_Default() {
        // Bidder mới khởi tạo → totalDeposit và totalWithdraw phải = 0
        assertEquals(0.0, bidder.getTotalDeposit(),  "totalDeposit mac dinh phai la 0");
        assertEquals(0.0, bidder.getTotalWithdraw(), "totalWithdraw mac dinh phai la 0");
    }

    @Test
    @DisplayName("Bidder: setTotalDeposit và setTotalWithdraw cập nhật đúng")
    void testBidderSetTotalDepositWithdraw() {
        bidder.setTotalDeposit(3_000_000);
        bidder.setTotalWithdraw(500_000);
        assertEquals(3_000_000, bidder.getTotalDeposit());
        assertEquals(500_000,   bidder.getTotalWithdraw());
    }

    @Test
    @DisplayName("Bidder là instance của User và Account")
    void testBidderPolymorphism() {
        assertInstanceOf(User.class,    bidder, "Bidder phai la User");
        assertInstanceOf(Account.class, bidder, "Bidder phai la Account");
    }


    // ═══════════════════════════════════════════════════════
    //  Seller
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("Seller: constructor khởi tạo đúng tất cả field")
    void testSellerConstructor() {
        assertEquals("2",          seller.getId());
        assertEquals("bob",        seller.getUsername());
        assertEquals("SELLER",     seller.getRole());
        assertEquals(5_000_000,    seller.getBalance());
    }

    @Test
    @DisplayName("Seller.displayRole() trả về 'Seller (Merchant)'")
    void testSellerDisplayRole() {
        assertEquals("Seller (Merchant)", seller.displayRole());
    }

    @Test
    @DisplayName("Seller: totalDeposit và totalWithdraw mặc định = 0")
    void testSellerTotalDepositWithdraw_Default() {
        assertEquals(0.0, seller.getTotalDeposit(),  "totalDeposit mac dinh phai la 0");
        assertEquals(0.0, seller.getTotalWithdraw(), "totalWithdraw mac dinh phai la 0");
    }

    @Test
    @DisplayName("Seller: setTotalDeposit và setTotalWithdraw cập nhật đúng")
    void testSellerSetTotalDepositWithdraw() {
        seller.setTotalDeposit(10_000_000);
        seller.setTotalWithdraw(2_000_000);
        assertEquals(10_000_000, seller.getTotalDeposit());
        assertEquals(2_000_000,  seller.getTotalWithdraw());
    }

    @Test
    @DisplayName("Seller là instance của User và Account")
    void testSellerPolymorphism() {
        assertInstanceOf(User.class,    seller, "Seller phai la User");
        assertInstanceOf(Account.class, seller, "Seller phai la Account");
    }


    // ═══════════════════════════════════════════════════════
    //  Admin
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("Admin: constructor khởi tạo đúng tất cả field")
    void testAdminConstructor() {
        assertEquals("3",              admin.getId());
        assertEquals("ADMIN",          admin.getRole());
        assertEquals("admin@mail.com", admin.getEmail());
    }

    @Test
    @DisplayName("Admin.displayRole() trả về 'System Administrator'")
    void testAdminDisplayRole() {
        assertEquals("System Administrator", admin.displayRole());
    }

    @Test
    @DisplayName("Admin là instance của Account, không có balance như User")
    void testAdminIsAccount_NotUser() {
        assertInstanceOf(Account.class, admin, "Admin phai la Account");
        // Admin extends Account trực tiếp, không qua User
        // → Admin không có balance, chỉ kiểm tra role
        assertEquals("ADMIN", admin.getRole());
        // Admin không phải Bidder hay Seller
        assertNotEquals("BIDDER", admin.getRole());
        assertNotEquals("SELLER", admin.getRole());
    }


    // ═══════════════════════════════════════════════════════
    //  Đa hình qua Account
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("Bidder, Seller, Admin có cùng kiểu cha Account — đa hình hoạt động")
    void testPolymorphicList() {
        // Tất cả đều có thể lưu vào mảng Account — đa hình
        Account[] accounts = { bidder, seller, admin };

        assertEquals("BIDDER", accounts[0].getRole());
        assertEquals("SELLER", accounts[1].getRole());
        assertEquals("ADMIN",  accounts[2].getRole());
    }

    @Test
    @DisplayName("displayRole() đa hình — mỗi class trả về chuỗi riêng")
    void testDisplayRole_Polymorphic() {
        Account[] accounts = { bidder, seller, admin };

        assertEquals("Bidder (Buyer)",       accounts[0].displayRole());
        assertEquals("Seller (Merchant)",    accounts[1].displayRole());
        assertEquals("System Administrator", accounts[2].displayRole());
    }

    @Test
    @DisplayName("setUsername và setPassword hoạt động trên Account")
    void testAccountSetters() {
        bidder.setUsername("alice_new");
        bidder.setPassword("newpass123");
        assertEquals("alice_new",   bidder.getUsername());
        assertEquals("newpass123",  bidder.getPassword());
    }
}