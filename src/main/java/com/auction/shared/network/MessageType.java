package com.auction.shared.network;

public enum MessageType {
    // ── Auth ──────────────────────────────────────────
    LOGIN,
    REGISTER,
    LOGOUT,
    FORGOT_PASSWORD,


    // ── Account / User ────────────────────────────────
    GET_ALL_USERS,
    UPDATE_PROFILE,
    RESET_PASSWORD,
    CHANGE_PASSWORD,
    FORGOT_PASSWORD_SUCCESS, // 👈 Cần thiết để kích hoạt chuyển màn hình
    FORGOT_PASSWORD_FAIL,    // 👈 Cần thiết để báo lỗi nếu sai email/user
    RESET_PASSWORD_SUCCESS,  // 👈 Cần thiết để báo đổi mật khẩu xong
    RESET_PASSWORD_FAIL,     // 👈 Cần thiết để báo lỗi OTP sai
    SWITCH_ROLE,  // Đổi vai trò

    GET_USER_BY_ID,
    UPDATE_USER_STATUS,
    GET_ACCOUNT_BY_ID,

    // ── Item (Product) ────────────────────────────────
    CREATE_ITEM,
    GET_ITEM_BY_ID,
    GET_ITEMS_BY_OWNER,
    UPDATE_ITEM,
    DELETE_ITEM,
    GET_PRODUCTS,

    // ── Auction ───────────────────────────────────────
    CREATE_AUCTION,
    GET_ALL_AUCTIONS,
    GET_AUCTION_BY_ID,
    GET_AUCTION_BY_ITEM_ID,
    GET_AUCTIONS_BY_BIDDER,
    GET_AUCTIONS_BY_SELLER,
    UPDATE_AUCTION_STATUS,
    CLOSE_AUCTION,
    GET_DASHBOARD_STATS,
    GET_HOT_AUCTIONS,
    LOCK_USER,


    // ── Bidding ───────────────────────────────────────
    PLACE_BID,
    GET_BID_HISTORY_STATS,
    SET_AUTO_BID,
    GET_BID_HISTORY,

    // ── Realtime (Observer qua Socket) ────────────────
    SUBSCRIBE_AUCTION,
    UNSUBSCRIBE_AUCTION,
    BID_UPDATE,          // Server → Client push khi có bid mới

    // ── Wallet / Transaction ──────────────────────────
    WALLET_TRANSACTION,
    GET_TRANSACTIONS,
    // 🌟 THÊM DÒNG NÀY VÀO ĐÂY: Dành riêng cho Server push Real-time
    UPDATE_PRICE
}