package com.auction.server.service;

import com.auction.server.dao.AccountDAO;
import com.auction.server.dao.AuctionDAO;
import com.auction.server.dao.AutoBidDAO;
import com.auction.server.dao.BidDAO;
import com.auction.server.dao.ItemDAO;
import com.auction.server.network.ClientHandler;
import com.auction.shared.model.*;
import com.auction.shared.network.Response;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionService {

    private final AuctionDAO auctionDAO = new AuctionDAO();
    private final BidDAO bidDAO = new BidDAO();
    private final ItemDAO itemDAO = new ItemDAO();
    private final AutoBidDAO autoBidDAO = new AutoBidDAO();
    private final AccountDAO accountDAO = new AccountDAO();

    // 🌟 TỐI ƯU 1: Hệ thống khóa phân mảnh theo từng ID phiên đấu giá
    private final ConcurrentHashMap<Integer, ReentrantLock> auctionLocks = new ConcurrentHashMap<>();

    private static final int SNIPE_WINDOW_SECONDS     = 30;
    private static final int EXTEND_SECONDS           = 60;
    private static final int MAX_TOTAL_EXTEND_SECONDS = 300;

    public boolean createAuction(Auction auction) {
        Item item = itemDAO.getItemById(auction.getItemId());
        if (item == null || !"AVAILABLE".equals(item.getStatus())) {
            System.err.println("Sản phẩm không hợp lệ hoặc đang nằm trong phiên khác!");
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
            System.err.println("Mức giá tự động tối đa phải lớn hơn giá hiện tại!");
            return false;
        }
        return autoBidDAO.registerAutoBid(auctionId, bidderId, maxBid);
    }

    /**
     * 🚀 ĐẶT GIÁ AN TOÀN ĐA LUỒNG
     */
    public boolean placeBid(int auctionId, double amount, Account account) {

        if (!(account instanceof User)) {
            System.err.println("Tài khoản quản trị viên không được phép đấu giá!");
            return false;
        }

        ReentrantLock auctionLock = auctionLocks.computeIfAbsent(auctionId, k -> new ReentrantLock());

        auctionLock.lock();
        try {
            User user = (User) account;
            Auction auction = auctionDAO.getAuctionById(auctionId);
            System.out.println("Check isActive: " + auction.isActive());
            System.out.println("Check minBid: " + (auction.getCurrentPrice() + auction.getMinIncrement()));
            System.out.println("Check balance: " + ((User)account).getBalance());

            if (auction == null || !auction.isActive()) {
                System.err.println("Phiên đấu giá không tồn tại hoặc chưa kích hoạt!");
                return false;
            }

            if (auction.getSellerId() == Integer.parseInt(account.getId())) {
                System.err.println("Chủ sở hữu sản phẩm không được tự đấu giá!");
                return false;
            }

            double minValidBid = auction.getCurrentPrice() + auction.getMinIncrement();
            if (amount < minValidBid) {
                System.err.println("Giá đặt tối thiểu phải là: " + minValidBid);
                return false;
            }

            if (user.getBalance() < amount) {
                System.err.println("Tài khoản không đủ số dư khả dụng!");
                return false;
            }

            BidTransaction bid = new BidTransaction();
            bid.setAuctionId(auctionId);
            bid.setBidderId(Integer.parseInt(account.getId()));
            bid.setBidAmount(amount);

            boolean transactionSuccess = auctionDAO.placeBidTransaction(bid, amount, Integer.parseInt(account.getId()));
            if (!transactionSuccess) return false;

            System.out.println("🔥 Người dùng " + account.getUsername() + " đặt giá thành công: " + amount + " đ");

            applyAntiSniping(auction);

            ClientHandler.pushBidUpdate(auctionId, amount, account.getUsername());

            // Chạy chuỗi Auto-bid ngầm
            processAutoBidsChain(auctionId, Integer.parseInt(account.getId()));

            return true;

        } finally {
            auctionLock.unlock();
        }
    }

    private void applyAntiSniping(Auction auction) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = auction.getEndTime();

        if (now.isAfter(endTime.minusSeconds(SNIPE_WINDOW_SECONDS)) && now.isBefore(endTime)) {
            LocalDateTime originalEndTime = auctionDAO.getOriginalEndTime(auction.getId());
            if (originalEndTime == null) originalEndTime = endTime;

            if (endTime.isBefore(originalEndTime.plusSeconds(MAX_TOTAL_EXTEND_SECONDS))) {
                LocalDateTime newEndTime = endTime.plusSeconds(EXTEND_SECONDS);
                auctionDAO.updateEndTime(auction.getId(), newEndTime);
                System.out.println("🛡️ [Anti-Sniping] Gia hạn phiên #" + auction.getId() + " thêm 60s -> " + newEndTime);
            } else {
                System.out.println("⚠️ [Anti-Sniping] Đã chạm mốc gia hạn kịch trần 5 phút của phiên này.");
            }
        }
    }

    private void processAutoBidsChain(int auctionId, int lastBidderId) {
        System.out.println("processAutoBidsChain called for auction: " + auctionId);

        Auction auction = auctionDAO.getAuctionById(auctionId);
        if (auction == null || !auction.isActive()) return;

        List<int[]> autoBids = autoBidDAO.getAutoBidsByAuction(auctionId);
        int[] highestEligibleAutoBid = null;
        double targetBidAmount = 0;

        double nextRequiredBid = auction.getCurrentPrice() + auction.getMinIncrement();

        for (int[] autoBid : autoBids) {
            int bidderId = autoBid[0];
            double maxBid = autoBid[1] / 100.0;

            if (bidderId == lastBidderId) continue;

            if (maxBid >= nextRequiredBid) {
                if (highestEligibleAutoBid == null || maxBid > (highestEligibleAutoBid[1] / 100.0)) {
                    highestEligibleAutoBid = autoBid;
                    targetBidAmount = nextRequiredBid;
                }
            }
        }

        if (highestEligibleAutoBid != null) {
            int botBidderId = highestEligibleAutoBid[0];

            BidTransaction bid = new BidTransaction();
            bid.setAuctionId(auctionId);
            bid.setBidderId(botBidderId);
            bid.setBidAmount(targetBidAmount);

            boolean success = auctionDAO.placeBidTransaction(bid, targetBidAmount, botBidderId);
            if (success) {
                String botUsername = accountDAO.getUsernameById(String.valueOf(botBidderId));
                System.out.println("🤖 [Auto-Bid] Hệ thống tự đặt giá hộ User #" + botUsername + ": " + targetBidAmount + " đ");

                System.out.println("pushBidUpdate từ placeBid");
                new Exception().printStackTrace();

                ClientHandler.pushBidUpdate(auctionId, targetBidAmount, "Tự động (User: " + botUsername + ")");

                processAutoBidsChain(auctionId, botBidderId);
            }
        }
    }

    public boolean closeAuction(int auctionId) {
        Auction auction = auctionDAO.getAuctionById(auctionId);
        if (auction == null) return false;

        boolean closed = auctionDAO.updateStatus(auctionId, Auction.AuctionStatus.FINISHED);
        if (closed) {
            itemDAO.updateStatus(auction.getItemId(), auction.getWinnerId() != null ? "SOLD" : "AVAILABLE");
            auctionLocks.remove(auctionId);
        }
        return closed;
    }

    public List<Auction> getActiveAuctions() {
        return auctionDAO.getAuctionsByStatus(Auction.AuctionStatus.RUNNING);
    }

    public List<Auction> getAuctionsBySeller(int sellerId) {
        return auctionDAO.getAuctionsBySeller(sellerId);
    }

    public Auction getAuctionById(int auctionId) {
        return auctionDAO.getAuctionById(auctionId);
    }

    public Auction getAuctionByItemId(String itemId) {
        try {
            return auctionDAO.getAuctionByItemId(itemId);
        } catch (Exception e) {
            System.err.println("Lỗi getAuctionByItemId: " + e.getMessage());
            return null;
        }
    }

    public List<BidTransaction> getBidHistory(int auctionId) {
        return bidDAO.getBidsByAuction(auctionId);
    }

    public boolean createAuction(String itemId, int sellerId, double startPrice, String startTimeStr, String endTimeStr) {
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
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

    public List<Auction> getAllAuctions() {
        return auctionDAO.getAllAuctions();
    }

    public List<Auction> getAuctionsByBidder(int bidderId) {
        return auctionDAO.getAuctionsByBidder(bidderId);
    }

    public Response placeBid(int auctionId, double amount, String userId) {
        try {
            System.out.println("PLACE_BID userId nhận được: " + userId);
            Account account = accountDAO.getAccountById((int) Double.parseDouble(userId));
            System.out.println("Account tìm được: " + account);
            if (account == null) {
                return new Response(false, "Tài khoản không tồn tại trên hệ thống!", null);
            }
            boolean ok = placeBid(auctionId, amount, account);
            return ok
                    ? new Response(true, "Đặt giá thành công!", null)
                    : new Response(false, "Đặt giá thất bại! Kiểm tra lại điều kiện đặt hoặc số dư.", null);
        } catch (Exception e) {
            return new Response(false, "Lỗi xử lý hệ thống: " + e.getMessage(), null);
        }
    }

    // ⚡ TỐI ƯU TOÀN DIỆN LỊCH SỬ THỐNG KÊ (Hạn chế kéo Object nặng qua Internet)
    public Map<String, Integer> getBidHistoryStats(int userId) {
        try {
            return auctionDAO.getDashboardStats(userId);
        } catch (Exception e) {
            Map<String, Integer> stats = new HashMap<>();
            stats.put("total", 0); stats.put("won", 0); stats.put("lost", 0);
            return stats;
        }
    }
    public boolean setAutoBid(int auctionId, String userId, double maxBid, double increment) {
        try {
            return registerAutoBid(auctionId, Integer.parseInt(userId), maxBid);
        } catch (Exception e) {
            return false;
        }
    }

    // ⚡ TỐI ƯU CHÍ MẠNG (ĐÃ SỬA): Loại bỏ việc gọi .size() từ list Object.
    // Chuyển hướng cho DB đếm trực tiếp số lượng phiên đang tham gia.
    public Map<String, Integer> getDashboardStats(int userId) {
        Map<String, Integer> stats = new java.util.HashMap<>();
        try {
            // Thay vì getAuctionsByBidder(userId).size(), ông cần tạo một hàm chuyên biệt trong auctionDAO
            // Nếu chưa viết hàm countActiveAuctionsByUser, tạm thời dùng bidDAO để đếm cho siêu tốc.
            int ongoing = auctionDAO.countActiveAuctionsByUser(userId);
            int won     = auctionDAO.countWonAuctions(userId);

            stats.put("ongoing", ongoing);
            stats.put("won",     won);
        } catch (Exception e) {
            // Cơ chế fallback phòng hờ lỗi: Đếm qua hàm gọn hơn
            try {
                int totalBids = bidDAO.countBidsByUser(userId);
                stats.put("ongoing", totalBids);
            } catch (Exception ignored) {
                stats.put("ongoing", 0);
            }
            stats.put("won", 0);
        }
        return stats;
    }

    public List<Auction> getAuctionsByStatus_wrapper(Auction.AuctionStatus status) {
        return auctionDAO.getAuctionsByStatus(status);
    }

}