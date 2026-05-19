package com.auction.server.service;

import com.auction.server.dao.AccountDAO;
import com.auction.server.dao.AuctionDAO;
import com.auction.server.dao.ItemDAO;
import com.auction.shared.model.Account;
import com.auction.shared.model.Auction;
import com.auction.shared.model.Bidder;
import com.auction.shared.model.Seller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionScheduler {

    private final AuctionDAO auctionDAO = new AuctionDAO();
    private final ItemDAO itemDAO = new ItemDAO();
    private final AccountDAO accountDAO = new AccountDAO();
    private final AuctionService auctionService = new AuctionService();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // Singleton
    private static AuctionScheduler instance;
    private AuctionScheduler() {}
    public static AuctionScheduler getInstance() {
        if (instance == null) {
            instance = new AuctionScheduler();
        }
        return instance;
    }

    // Gọi 1 lần khi khởi động app
    public void start() {
        scheduler.scheduleAtFixedRate(this::tick, 0, 30, TimeUnit.SECONDS);
        System.out.println("AuctionScheduler đã khởi động!");
    }

    // Dừng khi tắt app
    public void stop() {
        scheduler.shutdown();
        System.out.println("AuctionScheduler đã dừng!");
    }

    // Chạy mỗi 30 giây — kiểm tra tất cả phiên
    private void tick() {
        openPendingAuctions();
        closeExpiredAuctions();
    }

    // 1. Mở các phiên OPEN đã đến startTime → RUNNING
    private void openPendingAuctions() {
        List<Auction> openAuctions = auctionDAO.getAuctionsByStatus(Auction.AuctionStatus.OPEN);
        for (Auction auction : openAuctions) {
            if (auction.getStartTime() != null &&
                    LocalDateTime.now().isAfter(auction.getStartTime())) {

                auctionDAO.updateStatus(auction.getId(), Auction.AuctionStatus.RUNNING);
                System.out.println("Phiên " + auction.getId() + " bắt đầu!");
            }
        }
    }

    // 2. Đóng các phiên RUNNING đã hết endTime → FINISHED
    private void closeExpiredAuctions() {
        List<Auction> runningAuctions = auctionDAO.getAuctionsByStatus(Auction.AuctionStatus.RUNNING);
        for (Auction auction : runningAuctions) {
            if (auction.getEndTime() != null &&
                    LocalDateTime.now().isAfter(auction.getEndTime())) {

                closeAuction(auction);
            }
        }
    }

    // Đóng 1 phiên + xử lý tiền + notify UI
    private void closeAuction(Auction auction) {
        int auctionId = auction.getId();

        // Bước 1: Đổi status → FINISHED
        boolean closed = auctionDAO.updateStatus(auctionId, Auction.AuctionStatus.FINISHED);
        if (!closed) return;

        // Bước 2: Cập nhật Item + xử lý tiền
        if (auction.getWinnerId() != null) {
            itemDAO.updateStatus(auction.getItemId(), "SOLD");
            settlePayment(auction); // trừ tiền winner, cộng tiền seller
            System.out.println("Phiên " + auctionId + " kết thúc — Winner ID: " + auction.getWinnerId());
        } else {
            itemDAO.updateStatus(auction.getItemId(), "AVAILABLE");
            System.out.println("Phiên " + auctionId + " kết thúc — Không có người thắng");
        }

        // Bước 3: Notify tất cả màn hình đang xem phiên này
        auctionService.notifyObservers(auctionId, auction.getCurrentPrice(), "SYSTEM");
    }

    // Trừ tiền winner + cộng tiền seller
    private void settlePayment(Auction auction) {
        double price = auction.getCurrentPrice();

        // Lấy thông tin winner
        Account winner = accountDAO.getAccountById(auction.getWinnerId());
        if (winner instanceof Bidder) {
            double newBalance = ((Bidder) winner).getBalance() - price;
            if (newBalance < 0) {
                System.err.println("Winner không đủ tiền — cần xử lý thủ công!");
                return;
            }
            accountDAO.updateBalance(auction.getWinnerId(), newBalance);
            System.out.println("Trừ " + price + " từ winner ID: " + auction.getWinnerId());
        }

        // Lấy thông tin seller
        Account seller = accountDAO.getAccountById(auction.getSellerId());
        if (seller instanceof Seller) {
            double newBalance = ((Seller) seller).getBalance() + price;
            accountDAO.updateBalance(auction.getSellerId(), newBalance);
            System.out.println("Cộng " + price + " cho seller ID: " + auction.getSellerId());
        }
    }
}