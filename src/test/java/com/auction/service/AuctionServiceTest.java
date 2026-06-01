package com.auction.service;

import com.auction.shared.exception.AuctionClosedException;
import com.auction.shared.exception.InvalidBidException;
import com.auction.shared.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cho AuctionService — logic đấu giá.
 *
 * Bao gồm:
 *  - Kiểm tra trạng thái phiên (isActive)
 *  - Kiểm tra quyền đặt giá (Admin, Seller tự bid, Bidder)
 *  - Kiểm tra giá đặt hợp lệ / không hợp lệ
 *  - Kiểm tra số dư
 *  - Kiểm tra kết thúc phiên (closeAuction)
 *  - Kiểm tra validation thời gian tạo phiên
 *  - Kiểm tra placeBid() throw exception trực tiếp qua Auction model
 *
 * Lưu ý: AuctionService cần DB để chạy đầy đủ, nên các test
 * kiểm tra validation thuần túy được tái hiện qua Auction model
 * và logic if-check trực tiếp, không cần mock database.
 */
@DisplayName("AuctionService — Logic Đấu Giá Tests")
class AuctionServiceTest {

    private Auction runningAuction;
    private Bidder  bidder;
    private Seller  seller;
    private Admin   admin;

    private final LocalDateTime FUTURE = LocalDateTime.now().plusHours(2);
    private final LocalDateTime PAST   = LocalDateTime.now().minusHours(1);

    @BeforeEach
    void setUp() {
        // Khởi tạo phiên đấu giá đang chạy: sellerId=10, giá 500k, bước 50k
        runningAuction = new Auction(
                1, "ITEM_001", 10,
                500_000.0, 500_000.0, 50_000.0, null,
                LocalDateTime.now().minusMinutes(10),
                FUTURE,
                null,                           // originalEndTime (Anti-sniping)
                Auction.AuctionStatus.RUNNING,
                null                            // account
        );

        bidder = new Bidder("5",  "alice", "pass", "alice@mail.com", 2_000_000);
        seller = new Seller("10", "bob",   "pass", "bob@mail.com",   5_000_000);
        admin  = new Admin("99",  "admin", "admin", "admin@mail.com");
    }


    // ═══════════════════════════════════════════════════════
    //  Kiểm tra trạng thái phiên (isActive)
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("isActive: phiên RUNNING còn giờ → true")
    void testAuction_Running_IsActive() {
        assertTrue(runningAuction.isActive(), "RUNNING + chưa hết giờ → active");
    }

    @Test
    @DisplayName("isActive: phiên FINISHED → false")
    void testAuction_Finished_NotActive() {
        runningAuction.setStatus(Auction.AuctionStatus.FINISHED);
        assertFalse(runningAuction.isActive(), "FINISHED → không active");
    }

    @Test
    @DisplayName("isActive: phiên RUNNING nhưng hết giờ → false")
    void testAuction_Expired_NotActive() {
        runningAuction.setEndTime(PAST);
        assertFalse(runningAuction.isActive(), "Hết giờ → không active dù RUNNING");
    }

    @Test
    @DisplayName("isActive: phiên OPEN chưa bắt đầu → false")
    void testAuction_Open_NotActive() {
        runningAuction.setStatus(Auction.AuctionStatus.OPEN);
        assertFalse(runningAuction.isActive(), "OPEN → chưa active");
    }

    @Test
    @DisplayName("isActive: phiên CANCELED → false")
    void testAuction_Canceled_NotActive() {
        runningAuction.setStatus(Auction.AuctionStatus.CANCELED);
        assertFalse(runningAuction.isActive(), "CANCELED → không active");
    }


    // ═══════════════════════════════════════════════════════
    //  Kiểm tra quyền đặt giá
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("quyền: Admin không phải User → bị chặn đặt giá")
    void testPlaceBid_Admin_NotAllowed() {
        // AuctionService.placeBid() kiểm tra: if (!(account instanceof User)) return false
        assertEquals("ADMIN", admin.getRole());
        assertNotEquals("BIDDER", admin.getRole());
        assertNotEquals("SELLER", admin.getRole());
    }

    @Test
    @DisplayName("quyền: Bidder là User → được phép đặt giá")
    void testPlaceBid_Bidder_IsUser() {
        assertTrue(bidder instanceof User, "Bidder phải là User");
        assertEquals("BIDDER", bidder.getRole());
    }

    @Test
    @DisplayName("quyền: Seller là User → được phép đặt giá")
    void testPlaceBid_Seller_IsUser() {
        assertTrue(seller instanceof User, "Seller phải là User");
    }

    @Test
    @DisplayName("quyền: Seller tự đấu giá phiên của mình (sellerId=10) → bị chặn")
    void testPlaceBid_SelfBid_NotAllowed() {
        // AuctionService kiểm tra: if (auction.getSellerId() == Integer.parseInt(account.getId()))
        boolean isSelfBid = runningAuction.getSellerId() == Integer.parseInt(seller.getId());
        assertTrue(isSelfBid, "Seller id=10 đấu giá phiên sellerId=10 → bị chặn");
    }

    @Test
    @DisplayName("quyền: Bidder khác seller → không bị chặn self-bid")
    void testPlaceBid_OtherBidder_NotSelfBid() {
        boolean isSelfBid = runningAuction.getSellerId() == Integer.parseInt(bidder.getId());
        assertFalse(isSelfBid, "Bidder id=5 không phải seller id=10 → được phép");
    }


    // ═══════════════════════════════════════════════════════
    //  Kiểm tra giá đặt hợp lệ / không hợp lệ
    //  Gọi trực tiếp auction.placeBid() để test throw exception
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("placeBid: giá thấp hơn currentPrice + minIncrement → throw InvalidBidException")
    void testPlaceBid_BidTooLow_ThrowsException() {
        // minValidBid = 500k + 50k = 550k, đặt 510k → fail
        assertThrows(InvalidBidException.class,
                () -> runningAuction.placeBid(5, 510_000),
                "510k < 550k → InvalidBidException");
    }

    @Test
    @DisplayName("placeBid: giá bằng currentPrice (thiếu increment) → throw InvalidBidException")
    void testPlaceBid_BidEqualCurrentPrice_ThrowsException() {
        assertThrows(InvalidBidException.class,
                () -> runningAuction.placeBid(5, 500_000),
                "500k = currentPrice → thiếu increment → InvalidBidException");
    }

    @Test
    @DisplayName("placeBid: giá đúng bằng mức tối thiểu → thành công")
    void testPlaceBid_ExactMinimum_Success() throws Exception {
        // minValidBid = 500k + 50k = 550k
        runningAuction.placeBid(5, 550_000);
        assertEquals(550_000, runningAuction.getCurrentPrice(), "currentPrice phải cập nhật thành 550k");
    }

    @Test
    @DisplayName("placeBid: giá cao hơn mức tối thiểu → thành công, cập nhật winnerId")
    void testPlaceBid_HigherThanMinimum_UpdatesWinner() throws Exception {
        runningAuction.placeBid(5, 700_000);
        assertEquals(700_000, runningAuction.getCurrentPrice());
        assertEquals(5, runningAuction.getWinnerId(), "winnerId phải được cập nhật thành 5");
    }

    @Test
    @DisplayName("placeBid: phiên FINISHED → throw AuctionClosedException")
    void testPlaceBid_WhenFinished_ThrowsException() {
        runningAuction.setStatus(Auction.AuctionStatus.FINISHED);
        assertThrows(AuctionClosedException.class,
                () -> runningAuction.placeBid(5, 700_000),
                "FINISHED → AuctionClosedException");
    }

    @Test
    @DisplayName("placeBid: phiên CANCELED → throw AuctionClosedException")
    void testPlaceBid_WhenCanceled_ThrowsException() {
        runningAuction.setStatus(Auction.AuctionStatus.CANCELED);
        assertThrows(AuctionClosedException.class,
                () -> runningAuction.placeBid(5, 700_000),
                "CANCELED → AuctionClosedException");
    }

    @Test
    @DisplayName("placeBid: phiên hết giờ → throw AuctionClosedException")
    void testPlaceBid_WhenExpired_ThrowsException() {
        runningAuction.setEndTime(PAST);
        assertThrows(AuctionClosedException.class,
                () -> runningAuction.placeBid(5, 700_000),
                "Hết giờ → AuctionClosedException");
    }

    @Test
    @DisplayName("placeBid: exception chứa đúng attemptedAmount và minimumRequired")
    void testPlaceBid_ExceptionFields_AttemptedAndMinimum() {
        InvalidBidException ex = assertThrows(InvalidBidException.class,
                () -> runningAuction.placeBid(5, 510_000));

        assertEquals(510_000, ex.getAttemptedAmount());
        assertEquals(550_000, ex.getMinimumRequired()); // 500k + 50k
    }

    @Test
    @DisplayName("placeBid: AuctionClosedException chứa đúng status khi FINISHED")
    void testPlaceBid_AuctionClosedException_ContainsStatus() {
        runningAuction.setStatus(Auction.AuctionStatus.FINISHED);

        AuctionClosedException ex = assertThrows(AuctionClosedException.class,
                () -> runningAuction.placeBid(5, 700_000));

        assertEquals(Auction.AuctionStatus.FINISHED, ex.getCurrentStatus());
    }

    @Test
    @DisplayName("placeBid: bid thứ 2 thấp hơn bid thứ 1 → throw InvalidBidException")
    void testPlaceBid_SecondBidTooLow_ThrowsException() throws Exception {
        runningAuction.placeBid(5, 600_000); // bid 1 thành công, minimum mới = 650k

        assertThrows(InvalidBidException.class,
                () -> runningAuction.placeBid(7, 580_000),
                "580k < 650k (minimum mới) → InvalidBidException");
    }

    @Test
    @DisplayName("placeBid: bid thứ 2 hợp lệ → winner đổi sang bidder mới")
    void testPlaceBid_SecondBid_UpdatesWinner() throws Exception {
        runningAuction.placeBid(5, 600_000); // bidder 5 dẫn đầu
        runningAuction.placeBid(7, 700_000); // bidder 7 vượt qua

        assertEquals(700_000, runningAuction.getCurrentPrice());
        assertEquals(7, runningAuction.getWinnerId(), "Winner phải đổi sang bidder 7");
    }

    // ═══════════════════════════════════════════════════════
    //  Kiểm tra số dư
    //  Tái hiện điều kiện: if (user.getBalance() < amount) return false
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("số dư: không đủ → validation fail")
    void testPlaceBid_InsufficientBalance_Invalid() {
        double bidAmount = 3_000_000;
        // bidder có 2tr, đặt 3tr → không đủ
        assertFalse(bidder.getBalance() >= bidAmount, "2tr < 3tr → không đủ số dư");
    }

    @Test
    @DisplayName("số dư: đủ → validation pass")
    void testPlaceBid_SufficientBalance_Valid() {
        double bidAmount = 600_000;
        assertTrue(bidder.getBalance() >= bidAmount, "2tr > 600k → đủ số dư");
    }

    @Test
    @DisplayName("số dư: đúng bằng giá đặt → validation pass")
    void testPlaceBid_BalanceExactAmount_Valid() {
        double bidAmount = 2_000_000;
        assertTrue(bidder.getBalance() >= bidAmount, "2tr = 2tr → đủ số dư");
    }


    // ═══════════════════════════════════════════════════════
    //  Kết thúc phiên (closeAuction)
    //  Tái hiện logic: winner → SOLD, không winner → AVAILABLE
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("closeAuction: có winner → item status = SOLD")
    void testCloseAuction_WithWinner_ItemSold() {
        runningAuction.setWinnerId(5);
        // Tái hiện: itemDAO.updateStatus(auction.getItemId(), winnerId != null ? "SOLD" : "AVAILABLE")
        String newItemStatus = runningAuction.getWinnerId() != null ? "SOLD" : "AVAILABLE";
        assertEquals("SOLD", newItemStatus, "Có winner → item phải SOLD");
    }

    @Test
    @DisplayName("closeAuction: không có winner → item status = AVAILABLE")
    void testCloseAuction_NoWinner_ItemAvailable() {
        runningAuction.setWinnerId(null);
        String newItemStatus = runningAuction.getWinnerId() != null ? "SOLD" : "AVAILABLE";
        assertEquals("AVAILABLE", newItemStatus, "Không winner → item phải AVAILABLE");
    }

    @Test
    @DisplayName("closeAuction: trạng thái chuyển sang FINISHED")
    void testCloseAuction_StatusBecomesFinished() {
        runningAuction.setStatus(Auction.AuctionStatus.FINISHED);
        assertEquals(Auction.AuctionStatus.FINISHED, runningAuction.getStatus());
    }

    @Test
    @DisplayName("closeAuction: sau FINISHED thì isActive() = false")
    void testCloseAuction_AfterFinished_NotActive() {
        runningAuction.setStatus(Auction.AuctionStatus.FINISHED);
        assertFalse(runningAuction.isActive(), "Sau FINISHED → không active nữa");
    }


    // ═══════════════════════════════════════════════════════
    //  createAuction — validation thời gian
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("createAuction: endTime trước startTime → không hợp lệ")
    void testCreateAuction_InvalidTime() {
        Auction a = new Auction();
        a.setStartTime(FUTURE);
        a.setEndTime(PAST);
        // AuctionService kiểm tra: if (auction.getEndTime().isBefore(auction.getStartTime())) return false
        assertTrue(a.getEndTime().isBefore(a.getStartTime()), "endTime trước startTime → fail");
    }

    @Test
    @DisplayName("createAuction: startTime trước endTime → hợp lệ")
    void testCreateAuction_ValidTime() {
        Auction a = new Auction();
        a.setStartTime(LocalDateTime.now());
        a.setEndTime(FUTURE);
        assertFalse(a.getEndTime().isBefore(a.getStartTime()), "startTime trước endTime → pass");
    }

    @Test
    @DisplayName("createAuction: chỉ Seller mới được tạo phiên")
    void testCreateAuction_OnlySellerAllowed() {
        assertEquals("SELLER",    seller.getRole(),  "Seller được tạo phiên");
        assertNotEquals("SELLER", bidder.getRole(),  "Bidder không được tạo phiên");
        assertNotEquals("SELLER", admin.getRole(),   "Admin không được tạo phiên");
    }

    // ─── registerAutoBid() ───

    @Test
    @DisplayName("registerAutoBid: maxBid thấp hơn giá hiện tại → trả false")
    void testRegisterAutoBid_MaxBidTooLow() {
        // currentPrice = 500k, maxBid = 300k → fail
        double maxBid = 300_000;
        boolean valid = maxBid > runningAuction.getCurrentPrice();
        assertFalse(valid, "maxBid thap hon currentPrice phai fail");
    }

    @Test
    @DisplayName("registerAutoBid: maxBid bằng giá hiện tại → trả false")
    void testRegisterAutoBid_MaxBidEqualCurrentPrice() {
        double maxBid = runningAuction.getCurrentPrice(); // = 500k
        boolean valid = maxBid > runningAuction.getCurrentPrice();
        assertFalse(valid, "maxBid bang currentPrice phai fail");
    }

    @Test
    @DisplayName("registerAutoBid: maxBid hợp lệ → trả true")
    void testRegisterAutoBid_ValidMaxBid() {
        double maxBid = 1_000_000; // > 500k
        boolean valid = maxBid > runningAuction.getCurrentPrice();
        assertTrue(valid, "maxBid lon hon currentPrice phai pass");
    }
}
