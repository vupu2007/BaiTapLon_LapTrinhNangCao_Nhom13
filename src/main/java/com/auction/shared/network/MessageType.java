package com.auction.shared.network;

public enum MessageType {
    // ── Auth ──────────────────────────────────────────
    LOGIN,
    REGISTER,
    LOGOUT,

    // ── Account / User ────────────────────────────────
    GET_ALL_USERS,
    UPDATE_PROFILE,
    CHANGE_PASSWORD,
    GET_USER_BY_ID,
    UPDATE_USER_STATUS,
    GET_ACCOUNT_BY_ID,

    // ── Item (Product) ────────────────────────────────
    CREATE_ITEM,
    GET_ITEM_BY_ID,
    GET_ITEMS_BY_OWNER,
    UPDATE_ITEM,
    DELETE_ITEM,
    GET_PRODUCTS,        // 🌟 THÊM DÒNG NÀY VÀO ĐỂ HỢP THỨC HOÁ CODE CLIENT

    // ── Auction ───────────────────────────────────────
    CREATE_AUCTION,
    GET_ALL_AUCTIONS,
    GET_AUCTION_BY_ID,
    GET_AUCTION_BY_ITEM_ID,
    GET_AUCTIONS_BY_BIDDER,
    GET_AUCTIONS_BY_SELLER,
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