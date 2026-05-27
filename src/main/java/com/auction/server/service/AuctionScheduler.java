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
        scheduler.scheduleAtFixedRate(this::tick, 10, 60, TimeUnit.SECONDS);
        System.out.println("✅ AuctionScheduler da khoi dong thanh cong tren Server!");
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
            List<Auction> allAuctions = auctionDAO.getAllAuctionsWithConnection(conn);
            if (allAuctions == null) return;

            for (Auction auction : allAuctions) {
                if (auction.getStatus() == Auction.AuctionStatus.OPEN
                        && auction.getStartTime() != null
                        && LocalDateTime.now().isAfter(auction.getStartTime())) {
                    auctionDAO.updateStatusWithConnection(conn, auction.getId(), Auction.AuctionStatus.RUNNING);
                    System.out.println("Phiên " + auction.getId() + " bắt đầu!");
                } else if (auction.getStatus() == Auction.AuctionStatus.RUNNING
                        && auction.getEndTime() != null
                        && LocalDateTime.now().isAfter(auction.getEndTime())) {
                    closeAuction(auction, conn);
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi Scheduler: " + e.getMessage());
        }
    }

    private void closeAuction(Auction auction, Connection conn) {
        int auctionId = auction.getId();

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

        try {
            ClientHandler.pushBidUpdate(auctionId, auction.getCurrentPrice(), "[HỆ THỐNG] - KẾT THÚC!");
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