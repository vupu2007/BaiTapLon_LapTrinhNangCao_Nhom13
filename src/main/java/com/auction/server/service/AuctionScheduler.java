package com.auction.server.service;

import com.auction.server.dao.AccountDAO;
import com.auction.server.dao.AuctionDAO;
import com.auction.server.dao.ItemDAO;
import com.auction.server.network.ClientHandler;
import com.auction.shared.model.Account;
import com.auction.shared.model.Auction;
import com.auction.shared.model.Bidder;
import com.auction.shared.model.Seller;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.sql.Connection;
import com.auction.server.util.DatabaseConnection;

public class AuctionScheduler {

    private final AuctionDAO auctionDAO = new AuctionDAO();
    private final ItemDAO itemDAO = new ItemDAO();
    private final AccountDAO accountDAO = new AccountDAO();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Auction-Scheduler-Thread");
        t.setDaemon(true);
        return t;
    });

    private static AuctionScheduler instance;

    private AuctionScheduler() {
    }

    public static synchronized AuctionScheduler getInstance() {
        if (instance == null) {
            instance = new AuctionScheduler();
        }
        return instance;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                tick();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }, 3, 3, TimeUnit.SECONDS);
        System.out.println("✅ AuctionScheduler da khoi dong thanh cong !");
    }

    public void stop() {
        try {
            scheduler.shutdown();
            if (!scheduler.awaitTermination(3, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            System.out.println("AuctionScheduler da dung!");
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
    private void tick() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED); // ← thêm
            List<Auction> allAuctions = auctionDAO.getAllAuctionsWithConnection(conn);
            if (allAuctions == null) return;

            LocalDateTime now = LocalDateTime.now();

            // 🌟 SỬA ĐOẠN NÀY TRONG HÀM TICK() CỦA SERVER:
            for (Auction auction : allAuctions) {

                if (auction.getStatus() == Auction.AuctionStatus.OPEN && auction.getStartTime() != null) {

                    // Chuyển cả 2 mốc thời gian về dạng Mili giây để so sánh cho chuẩn 100%
                    long nowMillis = java.sql.Timestamp.valueOf(LocalDateTime.now()).getTime();
                    long startMillis = java.sql.Timestamp.valueOf(auction.getStartTime()).getTime();

                    // Chỉ cần thời gian hiện tại VƯỢT QUÁ hoặc BẰNG thời gian bắt đầu
                    if (nowMillis >= startMillis) {

                        auctionDAO.updateStatusWithConnection(conn, auction.getId(), Auction.AuctionStatus.RUNNING);
                        System.out.println("🟢 [SERVER REALTIME] Phiên " + auction.getId() + " bắt đầu!");

                        try {
                            ClientHandler.pushBidUpdate(auction.getId(), auction.getCurrentPrice(), "[HỆ THỐNG] - PHIÊN ĐẤU GIÁ BẮT ĐẦU!", auction.getEndTime());
                        } catch (Exception e) {
                            System.err.println("Không thể notify client mở cửa: " + e.getMessage());
                        }
                    }
                }
                // 🌟 LOGIC ĐÓNG CỬA (Giữ nguyên của nhóm sếp)
                else if (auction.getStatus() == Auction.AuctionStatus.RUNNING
                        && auction.getEndTime() != null
                        && !now.isBefore(auction.getEndTime())) {

                    closeAuction(auction, conn);
                }
            }
         } catch (Exception e) {
        System.err.println("Lỗi Scheduler: " + e.getMessage());
        e.printStackTrace();
        }
    }

    private void closeAuction(Auction auction, Connection conn) {
        int auctionId = auction.getId();

        // 1. Đọc dữ liệu mới nhất từ DB lên để đồng bộ đối tượng
        Auction freshAuction = auctionDAO.getAuctionById(auctionId);
        if (freshAuction != null) {
            auction.setWinnerId(freshAuction.getWinnerId());
            auction.setCurrentPrice(freshAuction.getCurrentPrice()); // Lấy luôn giá cuối cùng
        }

        try {
            if (auction.getWinnerId() != null && auction.getWinnerId() > 0) {
                boolean paymentSuccess = settlePayment(auction);

                if (paymentSuccess) {
                    auctionDAO.updateStatusWithConnection(conn, auctionId, Auction.AuctionStatus.FINISHED);
                    itemDAO.updateStatus(auction.getItemId(), "SOLD");
                    System.out.println("Phiên " + auctionId + " kết thúc — Winner ID: " + auction.getWinnerId());
                } else {
                    auctionDAO.updateStatusWithConnection(conn, auctionId, Auction.AuctionStatus.FINISHED);
                    itemDAO.updateStatus(auction.getItemId(), "AVAILABLE");
                    System.err.println("Phiên #" + auctionId + " đóng thất bại — Trả lại AVAILABLE!");
                }
            } else {
                auctionDAO.updateStatusWithConnection(conn, auctionId, Auction.AuctionStatus.FINISHED);
                itemDAO.updateStatus(auction.getItemId(), "AVAILABLE");
                System.out.println("Phiên " + auctionId + " kết thúc — Không có người thắng");
            }
        } catch (SQLException e) {
            System.err.println("Lỗi đóng phiên #" + auctionId + ": " + e.getMessage());
        }

        // 2. SỬA DÒNG NÀY: Đọc trạng thái FINISHED mới nhất từ DB hoặc ép trạng thái kết thúc để gửi xuống Client
        try {
            double finalPrice = freshAuction != null ? freshAuction.getCurrentPrice() : auction.getCurrentPrice();
            String winnerName = "[HỆ THỐNG] - KẾT THÚC!";
            if (auction.getWinnerId() != null && auction.getWinnerId() > 0) {
                Account winner = accountDAO.getAccountById(auction.getWinnerId());
                if (winner != null) winnerName = "[HỆ THỐNG] - KẾT THÚC! Người thắng: " + winner.getUsername();
            }
            ClientHandler.pushBidUpdate(auctionId, finalPrice, winnerName, auction.getEndTime());
        } catch (Exception e) {
            System.err.println("Không thể notify client: " + e.getMessage());
        }
    }
    private boolean settlePayment(Auction auction) {
        Integer winnerId = auction.getWinnerId();
        int sellerId = auction.getSellerId();
        double price = auction.getCurrentPrice();

        try {
            if (winnerId == null || winnerId <= 0) return false;

            Account winner = accountDAO.getAccountById(winnerId);
            if (!(winner instanceof Bidder)) return false;
            if (((Bidder) winner).getBalance() < price) return false;

            // Su dung hàm nguyen tu de khong bao gio reset total_deposit/total_withdraw ve 0
            boolean debitOk = accountDAO.executeAtomicWalletUpdate(winnerId, price, "WITHDRAW");
            if (!debitOk) return false;
            accountDAO.insertTransaction(winnerId, price, "AUCTION_PAYMENT_DEBIT");

            boolean creditOk = accountDAO.executeAtomicWalletUpdate(sellerId, price, "DEPOSIT");
            if (creditOk) {
                accountDAO.insertTransaction(sellerId, price, "AUCTION_PAYMENT_CREDIT");
            }
            return true;
        } catch (Exception e) {
            System.err.println("Loi tinh tien chuoi san: " + e.getMessage());
            return false;
        }
    }
}