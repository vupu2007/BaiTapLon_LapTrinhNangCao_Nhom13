package com.auction.shared.exception;

import java.io.Serial;

/**
 * Ném ra khi người dùng đặt giá không hợp lệ:
 * - Giá đặt thấp hơn hoặc bằng giá hiện tại
 * - Giá đặt không đủ bước tăng tối thiểu
 * - Giá đặt âm hoặc bằng 0
 */
public class InvalidBidException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;
    private final double attemptedAmount;
    private final double minimumRequired;

    public InvalidBidException(String message) {
        super(message);
        this.attemptedAmount = 0;
        this.minimumRequired = 0;
    }

    public InvalidBidException(String message, double attemptedAmount, double minimumRequired) {
        super(message);
        this.attemptedAmount = attemptedAmount;
        this.minimumRequired = minimumRequired;
    }

    public InvalidBidException(String message, double attemptedAmount,
                               double minimumRequired, Throwable cause) {
        super(message, cause);
        this.attemptedAmount = attemptedAmount;
        this.minimumRequired = minimumRequired;
    }

    public double getAttemptedAmount() { return attemptedAmount; }
    public double getMinimumRequired() { return minimumRequired; }
}
