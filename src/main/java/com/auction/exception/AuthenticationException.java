package com.auction.exception;

/**
 * Ném ra khi xác thực người dùng thất bại:
 * - Sai username hoặc password
 * - Tài khoản không tồn tại
 * - Không đủ quyền thực hiện thao tác
 */
public class AuthenticationException extends Exception {

    public enum Reason {
        INVALID_CREDENTIALS,   // Sai username/password
        ACCOUNT_NOT_FOUND,     // Không tìm thấy tài khoản
        UNAUTHORIZED_ACTION    // Không đủ quyền
    }

    private final Reason reason;

    public AuthenticationException(String message) {
        super(message);
        this.reason = Reason.INVALID_CREDENTIALS;
    }

    public AuthenticationException(String message, Reason reason) {
        super(message);
        this.reason = reason;
    }

    public Reason getReason() { return reason; }
}
