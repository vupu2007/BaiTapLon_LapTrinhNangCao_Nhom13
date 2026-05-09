package com.auction.service;

import com.auction.dao.AccountDAO;
import com.auction.dao.AuctionDAO;
import com.auction.dao.BidDAO;
import com.auction.dao.ItemDAO;
import com.auction.model.Account;
import com.auction.model.Auction;
import com.auction.model.Auction.AuctionStatus;
import com.auction.model.BidTransaction;
import com.auction.model.Item;
import com.auction.model.Seller;
import com.auction.model.User;
import com.auction.observer.AuctionObserver;
import com.auction.observer.AuctionSubject;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionService implements AuctionSubject {

    private final AuctionDAO auctionDAO = new AuctionDAO();
    private final BidDAO     bidDAO     = new BidDAO();
    private final ItemDAO    itemDAO    = new ItemDAO();
    private final AccountDAO accountDAO = new AccountDAO();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
    private final List<AuctionObserver> observers = new ArrayList<>();

    @Override
    public void addObserver(AuctionObserver observer) { observers.add(observer); }

    @Override
    public void removeObserver(AuctionObserver observer) { observers.remove(observer); }

    @Override
    public void notifyObservers(int auctionId, double newPrice) {
        for (AuctionObserver observer : observers) {
            observer.onBidPlaced(auctionId, newPrice);
        }
    }

    // 1. Tạo phiên đấu giá mới (chỉ Seller mới tạo được)
    public boolean createAuction(Auction auction, Account account) {
        if (!(account instanceof Seller)) {
            System.err.println("Chỉ Seller mới được tạo phiên đấu giá!");
            return false;
        }

        Item item = itemDAO.getItemById(auction.getItemId());
        if (item == null) {
            System.err.println("Sản phẩm không tồn tại!");
            return false;
        }
        if (!"AVAILABLE".equals(item.getStatus())) {
            System.err.println("Sản phẩm đang trong phiên đấu giá khác hoặc đã bán!");
            return false;
        }

        // THÊM: validate thời gian hợp lệ
        if (!auction.getStartTime().isBefore(auction.getEndTime())) {
            System.err.println("Thời gian bắt đầu phải trước thời gian kết thúc!");
            return false;
        }

        boolean created = auctionDAO.insertAuction(auction);
        if (created) {
            itemDAO.updateStatus(auction.getItemId(), "IN_AUCTION");
            scheduleAuction(auction);
        }
        return created;
    }

    // Lên lịch tự động OPEN→RUNNING và RUNNING→FINISHED
    private void scheduleAuction(Auction auction) {
        long startDelay = Duration.between(LocalDateTime.now(), auction.getStartTime()).toMillis();
        long endDelay   = Duration.between(LocalDateTime.now(), auction.getEndTime()).toMillis();

        // THÊM: tự động chuyển OPEN → RUNNING khi đến start_time
        if (startDelay > 0) {
            scheduler.schedule(() ->
                            auctionDAO.updateStatus(auction.getId(), AuctionStatus.RUNNING),
                    startDelay, TimeUnit.MILLISECONDS);
        } else {
            auctionDAO.updateStatus(auction.getId(), AuctionStatus.RUNNING);
        }

        // Tự động đóng phiên khi hết end_time
        if (endDelay > 0) {
            scheduler.schedule(() -> closeAuction(auction.getId()), endDelay, TimeUnit.MILLISECONDS);
        } else {
            closeAuction(auction.getId());
        }
    }

    // 2. Đặt giá — synchronized để tránh race condition
    public synchronized boolean placeBid(int auctionId, double amount, Account account) {
        if (!(account instanceof User)) {
            System.err.println("Admin không được phép đặt giá!");
            return false;
        }

        User user = (User) account;

        Auction auction = auctionDAO.getAuctionById(auctionId);
        if (auction == null || !auction.isActive()) {
            System.err.println("Phiên đấu giá không tồn tại hoặc đã kết thúc!");
            return false;
        }

        if (auction.getSellerId() == Integer.parseInt(account.getId())) {
            System.err.println("Không thể đấu giá sản phẩm của chính mình!");
            return false;
        }

        double minValidBid = auction.getCurrentPrice() + auction.getMinIncrement();
        if (amount < minValidBid) {
            System.err.println("Giá đặt phải lớn hơn " + minValidBid);
            return false;
        }

        if (user.getBalance() < amount) {
            System.err.println("Số dư không đủ!");
            return false;
        }

        int bidderId = Integer.parseInt(account.getId());

        BidTransaction bid = new BidTransaction();
        bid.setAuctionId(auctionId);
        bid.setBidderId(bidderId);
        bid.setBidAmount(amount);

        boolean success = auctionDAO.placeBidTransaction(bid, amount, bidderId);

        // THÊM: trừ tiền bidder sau khi đặt giá thành công
        if (success) {
            double newBalance = user.getBalance() - amount;
            accountDAO.updateBalance(bidderId, newBalance);
            user.setBalance(newBalance);
            notifyObservers(auctionId, amount);
        }

        return success;
    }

    // 3. Đóng phiên đấu giá (RUNNING → FINISHED)
    public boolean closeAuction(int auctionId) {
        Auction auction = auctionDAO.getAuctionById(auctionId);
        if (auction == null) {
            System.err.println("Phiên đấu giá không tồn tại!");
            return false;
        }

        boolean closed = auctionDAO.updateStatus(auctionId, AuctionStatus.FINISHED);
        if (closed) {
            String newItemStatus = auction.getWinnerId() != null ? "SOLD" : "AVAILABLE";
            itemDAO.updateStatus(auction.getItemId(), newItemStatus);

            // THÊM: hoàn tiền nếu không có winner
            if (auction.getWinnerId() == null) {
                refundBidders(auctionId);
            }

            System.out.println("Phiên đấu giá " + auctionId + " đã kết thúc!");
        }
        return closed;
    }

    // THÊM: hủy phiên (chỉ hủy được khi chưa có bid nào)
    public boolean cancelAuction(int auctionId, Account account) {
        Auction auction = auctionDAO.getAuctionById(auctionId);
        if (auction == null) {
            System.err.println("Phiên đấu giá không tồn tại!");
            return false;
        }
        if (auction.getSellerId() != Integer.parseInt(account.getId())) {
            System.err.println("Chỉ Seller tạo phiên mới được hủy!");
            return false;
        }
        if (bidDAO.getHighestBid(auctionId) != null) {
            System.err.println("Không thể hủy phiên đã có người đặt giá!");
            return false;
        }

        boolean canceled = auctionDAO.updateStatus(auctionId, AuctionStatus.CANCELED);
        if (canceled) {
            itemDAO.updateStatus(auction.getItemId(), "AVAILABLE");
        }
        return canceled;
    }

    // THÊM: hoàn tiền cho tất cả bidder khi phiên không có winner
    private void refundBidders(int auctionId) {
        List<BidTransaction> bids = bidDAO.getBidsByAuction(auctionId);
        for (BidTransaction bid : bids) {
            Account bidder = accountDAO.getAccountById(bid.getBidderId());
            if (bidder instanceof User) {
                double refunded = ((User) bidder).getBalance() + bid.getBidAmount();
                accountDAO.updateBalance(bid.getBidderId(), refunded);
            }
        }
    }

    // 4. Lấy danh sách phiên đấu giá đang chạy
    public List<Auction> getRunningAuctions() {
        return auctionDAO.getAuctionsByStatus(AuctionStatus.RUNNING);
    }

    // 5. Lấy lịch sử bid của một phiên
    public List<BidTransaction> getBidHistory(int auctionId) {
        return bidDAO.getBidsByAuction(auctionId);
    }
}