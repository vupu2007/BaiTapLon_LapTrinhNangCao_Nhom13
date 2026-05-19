package com.auction.shared.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class cho cây kế thừa User: Bidder, Seller, Admin.
 * Bao gồm: constructor, role, balance, displayRole(), đa hình.
 */
@DisplayName("User Hierarchy Tests (Bidder / Seller / Admin)")
class UserTest {

    // ───────── Bidder ─────────

    @Test
    @DisplayName("Bidder khởi tạo đúng với role = BIDDER")
    void testBidderConstructor() {
        Bidder bidder = new Bidder("1", "alice", "pass123", "alice@mail.com", 2_000_000);
        assertEquals("1", bidder.getId());
        assertEquals("alice", bidder.getUsername());
        assertEquals("BIDDER", bidder.getRole());
        assertEquals("alice@mail.com", bidder.getEmail());
        assertEquals(2_000_000, bidder.getBalance());
    }

    @Test
    @DisplayName("Bidder.displayRole() trả về đúng chuỗi")
    void testBidderDisplayRole() {
        Bidder bidder = new Bidder("1", "alice", "pass123", "alice@mail.com", 0);
        assertEquals("Bidder (Buyer)", bidder.displayRole());
    }

    @Test
    @DisplayName("Bidder.setBalance() cập nhật số dư đúng")
    void testBidderSetBalance() {
        Bidder bidder = new Bidder("1", "alice", "pass123", "alice@mail.com", 500_000);
        bidder.setBalance(1_500_000);
        assertEquals(1_500_000, bidder.getBalance());
    }

    @Test
    @DisplayName("Bidder là instance của User và Account")
    void testBidderPolymorphism() {
        Bidder bidder = new Bidder("1", "alice", "pass123", "alice@mail.com", 0);
        assertInstanceOf(User.class, bidder);
        assertInstanceOf(Account.class, bidder);
    }

    // ───────── Seller ─────────

    @Test
    @DisplayName("Seller khởi tạo đúng với role = SELLER")
    void testSellerConstructor() {
        Seller seller = new Seller("2", "bob", "pass456", "bob@mail.com", 5_000_000);
        assertEquals("2", seller.getId());
        assertEquals("bob", seller.getUsername());
        assertEquals("SELLER", seller.getRole());
        assertEquals(5_000_000, seller.getBalance());
    }

    @Test
    @DisplayName("Seller.displayRole() trả về đúng chuỗi")
    void testSellerDisplayRole() {
        Seller seller = new Seller("2", "bob", "pass456", "bob@mail.com", 0);
        assertEquals("Seller (Merchant)", seller.displayRole());
    }

    @Test
    @DisplayName("Seller là instance của User và Account")
    void testSellerPolymorphism() {
        Seller seller = new Seller("2", "bob", "pass456", "bob@mail.com", 0);
        assertInstanceOf(User.class, seller);
        assertInstanceOf(Account.class, seller);
    }

    // ───────── Admin ─────────

    @Test
    @DisplayName("Admin khởi tạo đúng với role = ADMIN")
    void testAdminConstructor() {
        Admin admin = new Admin("3", "admin", "adminpass", "admin@mail.com");
        assertEquals("3", admin.getId());
        assertEquals("ADMIN", admin.getRole());
        assertEquals("admin@mail.com", admin.getEmail());
    }

    @Test
    @DisplayName("Admin.displayRole() trả về đúng chuỗi")
    void testAdminDisplayRole() {
        Admin admin = new Admin("3", "admin", "adminpass", "admin@mail.com");
        assertEquals("System Administrator", admin.displayRole());
    }

    @Test
    @DisplayName("Admin là instance của Account, không có balance")
    void testAdminNotUser() {
        Admin admin = new Admin("3", "admin", "adminpass", "admin@mail.com");
        assertInstanceOf(Account.class, admin);
        assertEquals("ADMIN", admin.getRole());
    }

    // ───────── Đa hình qua Account ─────────

    @Test
    @DisplayName("Bidder và Seller có cùng kiểu cha Account — đa hình hoạt động")
    void testPolymorphicList() {
        Account[] accounts = {
            new Bidder("1", "alice", "p1", "a@b.com", 100),
            new Seller("2", "bob",   "p2", "b@c.com", 200),
            new Admin("3",  "admin", "p3", "c@d.com")
        };

        assertEquals("BIDDER", accounts[0].getRole());
        assertEquals("SELLER", accounts[1].getRole());
        assertEquals("ADMIN",  accounts[2].getRole());
    }

    @Test
    @DisplayName("setUsername và setPassword hoạt động trên Account")
    void testAccountSetters() {
        Bidder bidder = new Bidder("1", "alice", "oldpass", "alice@mail.com", 0);
        bidder.setUsername("alice_new");
        bidder.setPassword("newpass123");
        assertEquals("alice_new", bidder.getUsername());
        assertEquals("newpass123", bidder.getPassword());
    }
}
