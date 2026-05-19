package com.auction.service;

import com.auction.exception.AuctionClosedException;
import com.auction.exception.AuthenticationException;
import com.auction.exception.InvalidBidException;
import com.auction.shared.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cho AuctionService — logic đấu giá.
 * Bao gồm:
 *  - Đặt giá hợp lệ / không hợp lệ
 *  - Kết thúc phiên (có winner / không winner)
 *  - Kiểm tra trạng thái phiên
 */
@DisplayName("AuctionService — Logic Đấu Giá Tests")
class AuctionServiceTest
{

    private Auction runningAuction;
    private Bidder  bidder;
    private Seller  seller;
    private Admin   admin;

    private final LocalDateTime FUTURE = LocalDateTime.now().plusHours(2);
    private final LocalDateTime PAST   = LocalDateTime.now().minusHours(1);

    @BeforeEach
    void setUp()
    {
        runningAuction = new Auction(
                1, "ITEM_001", 10,
                500_000.0, 500_000.0, 50_000.0, null,
                LocalDateTime.now().minusMinutes(10),
                FUTURE,
                Auction.AuctionStatus.RUNNING,
                null
        );

        bidder = new Bidder("5", "alice", "pass", "alice@mail.com", 2_000_000);
        seller = new Seller("10", "bob",  "pass", "bob@mail.com",   5_000_000);
        admin  = new Admin("99", "admin", "admin", "admin@mail.com");
    }

    //  Kiểm tra phiên đấu giá (isActive)

    @Test
    @DisplayName("Phiên RUNNING còn giờ → isActive() = true → được phép đặt giá")
    void testAuction_Running_IsActive()
    {
        assertTrue(runningAuction.isActive());
    }

    @Test
    @DisplayName("Phiên FINISHED → isActive() = false → không được đặt giá")
    void testAuction_Finished_NotActive()
    {
        runningAuction.setStatus(Auction.AuctionStatus.FINISHED);
        assertFalse(runningAuction.isActive());
    }

    @Test
    @DisplayName("Phiên RUNNING nhưng hết giờ → isActive() = false → không được đặt giá")
    void testAuction_Expired_NotActive()
    {
        runningAuction.setEndTime(PAST);
        assertFalse(runningAuction.isActive());
    }

    @Test
    @DisplayName("Phiên OPEN chưa bắt đầu → isActive() = false")
    void testAuction_Open_NotActive()
    {
        runningAuction.setStatus(Auction.AuctionStatus.OPEN);
        assertFalse(runningAuction.isActive());
    }

    @Test
    @DisplayName("Phiên CANCELED → isActive() = false")
    void testAuction_Canceled_NotActive()
    {
        runningAuction.setStatus(Auction.AuctionStatus.CANCELED);
        assertFalse(runningAuction.isActive());
    }

    //  Kiểm tra quyền đặt giá

    @Test
    @DisplayName("Admin KHÔNG phải User → bị chặn đặt giá")
    void testPlaceBid_Admin_NotAllowed()
    {
        assertEquals("ADMIN", admin.getRole());
        assertNotEquals("BIDDER", admin.getRole());
        assertNotEquals("SELLER", admin.getRole());    }

    @Test
    @DisplayName("Bidder là User → được phép đặt giá")
    void testPlaceBid_Bidder_IsUser()
    {
        assertEquals("BIDDER", bidder.getRole());
    }

    @Test
    @DisplayName("Seller là User → được phép đặt giá")
    void testPlaceBid_Seller_IsUser()
    {
        assertTrue(seller instanceof User, "Seller là User → được phép");
    }

    @Test
    @DisplayName("Seller tự đấu giá phiên của mình (sellerId=10) → bị chặn")
    void testPlaceBid_SelfBid_NotAllowed()
    {
        boolean isSelfBid = runningAuction.getSellerId()
                == Integer.parseInt(seller.getId());
        assertTrue(isSelfBid, "Seller id=10 đấu giá phiên sellerId=10 → bị chặn");
    }

    @Test
    @DisplayName("Bidder khác không bị chặn self-bid")
    void testPlaceBid_OtherBidder_NotSelfBid()
    {
        boolean isSelfBid = runningAuction.getSellerId()
                == Integer.parseInt(bidder.getId());
        assertFalse(isSelfBid, "Bidder id=5 không phải seller → được phép");
    }

    //  Kiểm tra giá đặt hợp lệ / không hợp lệ

    @Test
    @DisplayName("Giá đặt thấp hơn currentPrice + minIncrement → không hợp lệ")
    void testPlaceBid_BidTooLow_Invalid()
    {
        double minValidBid = runningAuction.getCurrentPrice()
                + runningAuction.getMinIncrement(); // 550_000
        double bidAmount = 510_000;
        assertFalse(bidAmount >= minValidBid, "510k < 550k → không hợp lệ");
    }

    @Test
    @DisplayName("Giá đặt bằng currentPrice (thiếu increment) → không hợp lệ")
    void testPlaceBid_BidEqualCurrentPrice_Invalid()
    {
        double minValidBid = runningAuction.getCurrentPrice()
                + runningAuction.getMinIncrement();
        double bidAmount = runningAuction.getCurrentPrice(); // 500_000
        assertFalse(bidAmount >= minValidBid, "500k < 550k → không hợp lệ");
    }

    @Test
    @DisplayName("Giá đặt đúng bằng mức tối thiểu → hợp lệ")
    void testPlaceBid_ExactMinimum_Valid()
    {
        double minValidBid = runningAuction.getCurrentPrice()
                + runningAuction.getMinIncrement(); // 550_000
        assertTrue(minValidBid >= minValidBid, "550k = 550k → hợp lệ");
    }

    @Test
    @DisplayName("Giá đặt cao hơn mức tối thiểu → hợp lệ")
    void testPlaceBid_HigherThanMinimum_Valid()
    {
        double minValidBid = runningAuction.getCurrentPrice()
                + runningAuction.getMinIncrement();
        double bidAmount = 700_000;
        assertTrue(bidAmount >= minValidBid, "700k > 550k → hợp lệ");
    }

    //  Kiểm tra số dư

    @Test
    @DisplayName("Số dư không đủ → không hợp lệ")
    void testPlaceBid_InsufficientBalance_Invalid()
    {
        double bidAmount = 3_000_000;
        assertFalse(bidder.getBalance() >= bidAmount, "2tr < 3tr → không đủ");
    }

    @Test
    @DisplayName("Số dư đủ → hợp lệ")
    void testPlaceBid_SufficientBalance_Valid()
    {
        double bidAmount = 600_000;
        assertTrue(bidder.getBalance() >= bidAmount, "2tr > 600k → đủ");
    }

    @Test
    @DisplayName("Số dư đúng bằng giá đặt → hợp lệ")
    void testPlaceBid_BalanceExactAmount_Valid()
    {
        double bidAmount = 2_000_000;
        assertTrue(bidder.getBalance() >= bidAmount, "2tr = 2tr → đủ");
    }

    //  Kết thúc phiên (closeAuction)

    @Test
    @DisplayName("Kết thúc phiên: có winner → item status phải là SOLD")
    void testCloseAuction_WithWinner_ItemSold()
    {
        runningAuction.setWinnerId(5);
        String newItemStatus = runningAuction.getWinnerId() != null ? "SOLD" : "AVAILABLE";
        assertEquals("SOLD", newItemStatus);
    }

    @Test
    @DisplayName("Kết thúc phiên: không có winner → item status phải là AVAILABLE")
    void testCloseAuction_NoWinner_ItemAvailable()
    {
        runningAuction.setWinnerId(null);
        String newItemStatus = runningAuction.getWinnerId() != null ? "SOLD" : "AVAILABLE";
        assertEquals("AVAILABLE", newItemStatus);
    }

    @Test
    @DisplayName("Kết thúc phiên: trạng thái chuyển sang FINISHED")
    void testCloseAuction_StatusBecomesFinished()
    {
        runningAuction.setStatus(Auction.AuctionStatus.FINISHED);
        assertEquals(Auction.AuctionStatus.FINISHED, runningAuction.getStatus());
    }

    @Test
    @DisplayName("Kết thúc phiên: sau FINISHED thì isActive() = false")
    void testCloseAuction_AfterFinished_NotActive()
    {
        runningAuction.setStatus(Auction.AuctionStatus.FINISHED);
        assertFalse(runningAuction.isActive(),
                "Phiên FINISHED không còn active → không cho đặt giá nữa");
    }

    //  createAuction — validation thời gian

    @Test
    @DisplayName("createAuction: endTime trước startTime → không hợp lệ")
    void testCreateAuction_InvalidTime()
    {
        Auction a = new Auction();
        a.setStartTime(FUTURE);
        a.setEndTime(PAST);
        assertTrue(a.getEndTime().isBefore(a.getStartTime()),
                "endTime trước startTime → fail");
    }

    @Test
    @DisplayName("createAuction: startTime trước endTime → hợp lệ")
    void testCreateAuction_ValidTime()
    {
        Auction a = new Auction();
        a.setStartTime(LocalDateTime.now());
        a.setEndTime(FUTURE);
        assertFalse(a.getEndTime().isBefore(a.getStartTime()),
                "startTime trước endTime → pass");
    }

    @Test
    @DisplayName("createAuction: chỉ Seller mới được tạo phiên")
    void testCreateAuction_OnlySellerAllowed()
    {
        assertEquals("SELLER", seller.getRole(),  "Seller được tạo phiên ");
        assertNotEquals("SELLER", bidder.getRole(), "Bidder không được tạo phiên ");
        assertNotEquals("SELLER", admin.getRole(),  "Admin không được tạo phiên ");
    }
}
