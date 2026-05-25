package com.auction.server.service;

import com.auction.server.dao.AuctionDAO;
import com.auction.server.dao.AutoBidDAO;
import com.auction.server.dao.BidDAO;
import com.auction.server.dao.ItemDAO;
import com.auction.shared.model.*;
import com.auction.shared.network.Response;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionService {

    private final AuctionDAO auctionDAO = new AuctionDAO();
    private final BidDAO bidDAO = new BidDAO();
    private final ItemDAO itemDAO = new ItemDAO();
    private final ReentrantLock lock = new ReentrantLock();
    private final AutoBidDAO autoBidDAO = new AutoBidDAO();

    private static final int SNIPE_WINDOW_SECONDS     = 30;
    private static final int EXTEND_SECONDS           = 60;
    private static final int MAX_TOTAL_EXTEND_SECONDS = 300;

    // 1. Seller tạo phiên đấu giá mới
    public boolean createAuction(Auction auction) {
        Item item = itemDAO.getItemById(auction.getItemId());
        if (item == null) {
            System.err.println("Sản phẩm không tồn tại!");
            return false;
        }
        if (!"AVAILABLE".equals(item.getStatus())) {
            System.err.println("Sản phẩm không ở trạng thái AVAILABLE!");
            return false;
        }
        if (auction.getEndTime().isBefore(auction.getStartTime())) {
            System.err.println("Thời gian kết thúc phải sau thời gian bắt đầu!");
            return false;
        }

        boolean created = auctionDAO.insertAuction(auction);
        if (created) {
            itemDAO.updateStatus(auction.getItemId(), "IN_AUCTION");
            System.out.println("Tạo phiên đấu giá thành công!");
        }
        return created;
    }

    public boolean registerAutoBid(int auctionId, int bidderId, double maxBid) {
        Auction auction = auctionDAO.getAuctionById(auctionId);
        if (auction == null || !auction.isActive()) {
            System.err.println("Phiên không tồn tại hoặc đã kết thúc!");
            return false;
        }
        if (maxBid <= auction.getCurrentPrice()) {
            System.err.println("maxBid phải lớn hơn giá hiện tại!");
            return false;
        }
        return autoBidDAO.registerAutoBid(auctionId, bidderId, maxBid);
    }

    // Observer
    private Map<Integer, List<Observer>> auctionObservers = new ConcurrentHashMap<>();

    public void addObserver(int auctionId, Observer observer) {
        auctionObservers.putIfAbsent(auctionId, new CopyOnWriteArrayList<>());
        auctionObservers.get(auctionId).add(observer);
    }

    public void removeObserver(int auctionId, Observer observer) {
        List<Observer> viewers = auctionObservers.get(auctionId);
        if (viewers != null) {
            viewers.remove(observer);
            if (viewers.isEmpty()) {
                auctionObservers.remove(auctionId);
            }
        }
    }

    public void notifyObservers(int auctionId, double newPrice, String username) {
        List<Observer> viewers = auctionObservers.get(auctionId);
        if (viewers != null) {
            for (Observer o : viewers) {
                o.update(newPrice, username);
            }
        }
    }

    // 2. Đặt giá
    public boolean placeBid(int auctionId, double amount, Account account) {
        lock.lock();
        try {
            if (!(account instanceof User)) {
                System.err.println("Admin không được phép đặt giá!");
                return false;
            }

            User user = (User) account;

            Auction auction = auctionDAO.getAuctionById(auctionId);
            if (auction == null) {
                System.err.println("Phiên đấu giá không tồn tại!");
                return false;
            }

            if (!auction.isActive()) {
                System.err.println("Phiên đấu giá đã kết thúc hoặc chưa bắt đầu!");
                return false;
            }

            if (auction.getSellerId() == Integer.parseInt(account.getId())) {
                System.err.println("Không thể đấu giá sản phẩm của chính mình!");
                return false;
            }

            double minValidBid = auction.getCurrentPrice() + auction.getMinIncrement();
            if (amount < minValidBid) {
                System.err.println("Giá đặt phải ít nhất là: " + minValidBid);
                return false;
            }

            if (user.getBalance() < amount) {
                System.err.println("Số dư không đủ!");
                return false;
            }

            BidTransaction bid = new BidTransaction();
            bid.setAuctionId(auctionId);
            bid.setBidderId(Integer.parseInt(account.getId()));
            bid.setBidAmount(amount);

            boolean transactionSuccess = auctionDAO.placeBidTransaction(bid, amount, Integer.parseInt(account.getId()));
            if (!transactionSuccess) return false;

            System.out.println("Đặt giá thành công: " + amount);

            // Anti-sniping: bid trong 30 giây cuối → gia hạn thêm 60 giây
            applyAntiSniping(auction);

            notifyObservers(auctionId, amount, account.getUsername());

            processAutoBids(auctionId, Integer.parseInt(account.getId()));

            return true;

        } finally {
            lock.unlock();
        }
    }

    private void applyAntiSniping(Auction auction) {
        LocalDateTime now     = LocalDateTime.now();
        LocalDateTime endTime = auction.getEndTime();

        // Nếu endTime còn hơn 5 phút so với now → đã gia hạn đủ rồi
        if (endTime.isAfter(now.plusSeconds(MAX_TOTAL_EXTEND_SECONDS))) {
            System.out.println("Phiên " + auction.getId() + " đã đạt giới hạn gia hạn 5 phút!");
            return;
        }

        // Bid trong 30 giây cuối → gia hạn thêm 60 giây
        if (now.isAfter(endTime.minusSeconds(SNIPE_WINDOW_SECONDS))) {
            LocalDateTime newEndTime = endTime.plusSeconds(EXTEND_SECONDS);
            auctionDAO.updateEndTime(auction.getId(), newEndTime);
            System.out.println("Anti-sniping: gia hạn phiên " + auction.getId()
                    + " đến " + newEndTime);
        }
    }

    private void processAutoBids(int auctionId, int lastBidderId) {
        Auction auction = auctionDAO.getAuctionById(auctionId);
        if (auction == null || !auction.isActive()) return;

        List<int[]> autoBids = autoBidDAO.getAutoBidsByAuction(auctionId);
        for (int[] autoBid : autoBids) {
            int bidderId  = autoBid[0];
            double maxBid = autoBid[1] / 100.0;

            if (bidderId == lastBidderId) continue;

            double nextBid = auction.getCurrentPrice() + auction.getMinIncrement();
            if (maxBid >= nextBid) {
                BidTransaction bid = new BidTransaction();
                bid.setAuctionId(auctionId);
                bid.setBidderId(bidderId);
                bid.setBidAmount(nextBid);

                boolean success = auctionDAO.placeBidTransaction(bid, nextBid, bidderId);
                if (success) {
                    System.out.println("Auto-bid: bidder " + bidderId + " tự động đặt " + nextBid);
                    notifyObservers(auctionId, nextBid, "Auto-bid");
                }
                break;
            }
        }
    }

    // 3. Đóng phiên
    public boolean closeAuction(int auctionId) {
        Auction auction = auctionDAO.getAuctionById(auctionId);
        if (auction == null) {
            System.err.println("Phiên đấu giá không tồn tại!");
            return false;
        }

        boolean closed = auctionDAO.updateStatus(auctionId, Auction.AuctionStatus.FINISHED);
        if (closed) {
            if (auction.getWinnerId() != null) {
                itemDAO.updateStatus(auction.getItemId(), "SOLD");
            } else {
                itemDAO.updateStatus(auction.getItemId(), "AVAILABLE");
            }
            System.out.println("Phiên đấu giá đã kết thúc!");
        }
        return closed;
    }

    // 4. Lấy danh sách phiên đang chạy
    public List<Auction> getActiveAuctions() {
        return auctionDAO.getAuctionsByStatus(Auction.AuctionStatus.RUNNING);
    }

    // 5. Lấy danh sách phiên của một Seller
    public List<Auction> getAuctionsBySeller(int sellerId) {
        return auctionDAO.getAuctionsBySeller(sellerId);
    }

    // 6. Lấy chi tiết một phiên
    public Auction getAuctionById(int auctionId) {
        return auctionDAO.getAuctionById(auctionId);
    }

    // 7. Lấy lịch sử bid của một phiên
    public List<BidTransaction> getBidHistory(int auctionId) {
        return bidDAO.getBidsByAuction(auctionId);
    }

    // ── Alias/wrapper methods cho ClientHandler ───────────────────────────────

    // createAuction(String itemId, int sellerId, double startPrice, String startTimeStr, String endTimeStr)
    // ClientHandler truyen tham so roi, khong truyen Auction object
    public boolean createAuction(String itemId, int sellerId, double startPrice,
                                 String startTimeStr, String endTimeStr) {
        java.time.format.DateTimeFormatter fmt =
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        Auction auction = new Auction();
        auction.setItemId(itemId);
        auction.setSellerId(sellerId);
        auction.setStartPrice(startPrice);
        auction.setCurrentPrice(startPrice);
        auction.setStartTime(java.time.LocalDateTime.parse(startTimeStr, fmt));
        auction.setEndTime(java.time.LocalDateTime.parse(endTimeStr, fmt));
        auction.setStatus(Auction.AuctionStatus.OPEN);
        return createAuction(auction);
    }

    // getAllAuctions: lay tat ca phien (OPEN + RUNNING + FINISHED)
    public List<Auction> getAllAuctions() {
        return auctionDAO.getAllAuctions();
    }

    // getAuctionsByBidder: lay danh sach phien bidder da tham gia
    public List<Auction> getAuctionsByBidder(int bidderId) {
        return auctionDAO.getAuctionsByBidder(bidderId);
    }

    // placeBid(int, double, String userId) — ClientHandler truyen userId la String
    // Wrapper lay Account tu DB roi goi placeBid goc
    public Response placeBid(int auctionId, double amount, String userId) {
        try {
            com.auction.server.dao.AccountDAO accountDAO = new com.auction.server.dao.AccountDAO();
            Account account = accountDAO.getAccountById(Integer.parseInt(userId));
            if (account == null)
                return new com.auction.shared.network.Response(false, "Tai khoan khong ton tai!", null);

            boolean ok = placeBid(auctionId, amount, account);
            return ok
                    ? new com.auction.shared.network.Response(true,  "Dat gia thanh cong!", null)
                    : new com.auction.shared.network.Response(false, "Dat gia that bai!", null);
        } catch (Exception e) {
            return new com.auction.shared.network.Response(false, "Loi server: " + e.getMessage(), null);
        }
    }

    // getBidHistoryStats: tra ve Map {total, won, lost} cho HistoryController
    public java.util.Map<String, Integer> getBidHistoryStats(int userId) {
        java.util.Map<String, Integer> stats = new java.util.HashMap<>();
        try {
            int total = bidDAO.countBidsByUser(userId);
            int won   = auctionDAO.countWonAuctions(userId);
            stats.put("total", total);
            stats.put("won",   won);
            stats.put("lost",  Math.max(0, total - won));
        } catch (Exception e) {
            stats.put("total", 0);
            stats.put("won",   0);
            stats.put("lost",  0);
        }
        return stats;
    }

    // setAutoBid: wrapper cho registerAutoBid, ClientHandler truyen String userId
    public boolean setAutoBid(int auctionId, String userId, double maxBid, double increment) {
        try {
            return registerAutoBid(auctionId, Integer.parseInt(userId), maxBid);
        } catch (Exception e) {
            return false;
        }
    }

    // getDashboardStats: thong ke cho man hinh chinh cua Bidder
    public java.util.Map<String, Integer> getDashboardStats(int userId) {
        java.util.Map<String, Integer> stats = new java.util.HashMap<>();
        try {
            int ongoing = getAuctionsByBidder(userId).size();
            int won     = auctionDAO.countWonAuctions(userId);
            stats.put("ongoing", ongoing);
            stats.put("won",     won);
        } catch (Exception e) {
            stats.put("ongoing", 0);
            stats.put("won",     0);
        }
        return stats;
    }

    // getAuctionsByStatus_wrapper: expose getAuctionsByStatus cho ClientHandler
    public List<Auction> getAuctionsByStatus_wrapper(Auction.AuctionStatus status) {
        return auctionDAO.getAuctionsByStatus(status);
    }
}