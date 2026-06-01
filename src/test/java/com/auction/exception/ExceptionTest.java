package com.auction.exception;

import com.auction.shared.model.Auction.AuctionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import com.auction.shared.exception.InvalidBidException;
import com.auction.shared.exception.AuctionClosedException;
import com.auction.shared.exception.AuthenticationException;

/**
 * Test cho 3 customs Exception của hệ thống đấu giá.
 */
@DisplayName("Custom Exception Tests")
class ExceptionTest {

    //  InvalidBidException

    @Test
    @DisplayName("InvalidBidException lưu đúng message")
    void testInvalidBidException_Message() {
        InvalidBidException ex = new InvalidBidException("Giá đặt quá thấp!");
        assertEquals("Giá đặt quá thấp!", ex.getMessage());
    }

    @Test
    @DisplayName("InvalidBidException lưu đúng attemptedAmount và minimumRequired")
    void testInvalidBidException_Fields() {
        InvalidBidException ex = new InvalidBidException("Lỗi giá", 300_000, 550_000);
        assertEquals(300_000, ex.getAttemptedAmount());
        assertEquals(550_000, ex.getMinimumRequired());
    }

    @Test
    @DisplayName("InvalidBidException là subclass của Exception")
    void testInvalidBidException_IsException() {
        InvalidBidException ex = new InvalidBidException("msg");
        assertInstanceOf(Exception.class, ex);
    }


    //  AuctionClosedException

    @Test
    @DisplayName("AuctionClosedException lưu đúng message")
    void testAuctionClosedException_Message() {
        AuctionClosedException ex = new AuctionClosedException("Phiên đã đóng!");
        assertEquals("Phiên đã đóng!", ex.getMessage());
    }

    @Test
    @DisplayName("AuctionClosedException lưu đúng auctionId và currentStatus")
    void testAuctionClosedException_Fields() {
        AuctionClosedException ex = new AuctionClosedException(
                "Phiên đã kết thúc", 5, AuctionStatus.FINISHED);
        assertEquals(5, ex.getAuctionId());
        assertEquals(AuctionStatus.FINISHED, ex.getCurrentStatus());
    }

    @Test
    @DisplayName("AuctionClosedException là subclass của Exception")
    void testAuctionClosedException_IsException() {
        AuctionClosedException ex = new AuctionClosedException("msg");
        assertInstanceOf(Exception.class, ex);
    }

    @Test
    @DisplayName("AuctionClosedException với status CANCELED")
    void testAuctionClosedException_CanceledStatus() {
        AuctionClosedException ex = new AuctionClosedException("Bị hủy", 3, AuctionStatus.CANCELED);
        assertEquals(AuctionStatus.CANCELED, ex.getCurrentStatus());
    }

    //  AuthenticationException

    @Test
    @DisplayName("AuthenticationException lưu đúng message")
    void testAuthenticationException_Message() {
        AuthenticationException ex = new AuthenticationException("Sai mật khẩu!");
        assertEquals("Sai mật khẩu!", ex.getMessage());
    }

    @Test
    @DisplayName("AuthenticationException mặc định reason là INVALID_CREDENTIALS")
    void testAuthenticationException_DefaultReason() {
        AuthenticationException ex = new AuthenticationException("Sai tài khoản");
        assertEquals(AuthenticationException.Reason.INVALID_CREDENTIALS, ex.getReason());
    }

    @Test
    @DisplayName("AuthenticationException lưu đúng Reason khi truyền vào")
    void testAuthenticationException_CustomReason() {
        AuthenticationException ex = new AuthenticationException(
                "Không đủ quyền", AuthenticationException.Reason.UNAUTHORIZED_ACTION);
        assertEquals(AuthenticationException.Reason.UNAUTHORIZED_ACTION, ex.getReason());
    }

    @Test
    @DisplayName("AuthenticationException với reason ACCOUNT_NOT_FOUND")
    void testAuthenticationException_AccountNotFound() {
        AuthenticationException ex = new AuthenticationException(
                "Không tìm thấy", AuthenticationException.Reason.ACCOUNT_NOT_FOUND);
        assertEquals(AuthenticationException.Reason.ACCOUNT_NOT_FOUND, ex.getReason());
    }

    @Test
    @DisplayName("AuthenticationException là subclass của Exception")
    void testAuthenticationException_IsException() {
        AuthenticationException ex = new AuthenticationException("msg");
        assertInstanceOf(Exception.class, ex);
    }

    @Test
    @DisplayName("Tất cả 3 Reason enum tồn tại đúng")
    void testAuthenticationException_AllReasons() {
        assertEquals(3, AuthenticationException.Reason.values().length);
        assertNotNull(AuthenticationException.Reason.valueOf("INVALID_CREDENTIALS"));
        assertNotNull(AuthenticationException.Reason.valueOf("ACCOUNT_NOT_FOUND"));
        assertNotNull(AuthenticationException.Reason.valueOf("UNAUTHORIZED_ACTION"));
    }
}
