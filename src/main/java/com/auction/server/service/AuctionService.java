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
    // Giúp phiên #1 và phiên #2 đấu giá song song 100% không làm nghẽn luồng của nhau
    private final ConcurrentHashMap<Integer, ReentrantLock> auctionLocks = new ConcurrentHashMap<>();

    private static final int SNIPE_WINDOW_SECONDS     = 30;
    private static final int EXTEND_SECONDS           = 60;
    private static final int MAX_TOTAL_EXTEND_SECONDS = 300; // Giới hạn tổng thời gian gia hạn tối đa 5 phút

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
     * 🚀 ĐẶT GIÁ AN TOÀN ĐA LUỒNG: Sử dụng khóa cục bộ để tối ưu hiệu năng cao độ
     */
    public boolean placeBid(int auctionId, double amount, Account account) {
        if (!(account instanceof User)) {
            System.err.println("Tài khoản quản trị viên không được phép đấu giá!");
            return false;
        }

        // Lấy hoặc tạo một ổ khóa riêng biệt duy nhất cho ID phiên này
        ReentrantLock auctionLock = auctionLocks.computeIfAbsent(auctionId, k -> new ReentrantLock());

        auctionLock.lock();
        try {
            User user = (User) account;
            Auction auction = auctionDAO.getAuctionById(auctionId);

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

            // Ghi nhận giao dịch đặt giá vào DB
            BidTransaction bid = new BidTransaction();
            bid.setAuctionId(auctionId);
            bid.setBidderId(Integer.parseInt(account.getId()));
            bid.setBidAmount(amount);

            boolean transactionSuccess = auctionDAO.placeBidTransaction(bid, amount, Integer.parseInt(account.getId()));
            if (!transactionSuccess) return false;

            System.out.println("🔥 Người dùng " + account.getUsername() + " đặt giá thành công: " + amount + " đ");

            // Kiểm tra áp dụng luật bảo vệ chống Sniping phút chót
            applyAntiSniping(auction);

            // Bắn tín hiệu Real-time báo số tiền mới ngay lập tức cho toàn trạm mạng
            ClientHandler.pushBidUpdate(auctionId, amount, account.getUsername());

            // 🚀 BƯỚC NGOẶT: Kích hoạt chuỗi xử lý Auto-bid liên hoàn mà không bị ngắt quãng
            processAutoBidsChain(auctionId, Integer.parseInt(account.getId()));

            return true;

        } finally {
            auctionLock.unlock();
        }
    }

    /**
     * 🌟 SỬA LỖI ANTI-SNIPING: Tính toán dựa trên độ lệch gốc ban đầu của phiên đấu giá
     */
    private void applyAntiSniping(Auction auction) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = auction.getEndTime();

        // Kiểm tra xem thời gian hiện tại có đang nằm trong khung 30 giây cuối cùng hay không
        if (now.isAfter(endTime.minusSeconds(SNIPE_WINDOW_SECONDS)) && now.isBefore(endTime)) {

            // Lấy thông tin thời gian kết thúc gốc lúc tạo phiên từ DB để tính giới hạn kéo dài tối đa
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

    /**
     * 🌟 SỬA LỖI AUTO-BID LOOP: Kích hoạt chuỗi phản ứng liên hoàn (Chain Reaction)
     * Cho phép các bot Auto-bid tự động nâng giá đấu đá nhau cho tới khi chạm đỉnh trần
     */
    private void processAutoBidsChain(int auctionId, int lastBidderId) {
        // Đọc lại trạng thái phiên mới nhất sau mỗi lượt tăng giá
        Auction auction = auctionDAO.getAuctionById(auctionId);
        if (auction == null || !auction.isActive()) return;

        List<int[]> autoBids = autoBidDAO.getAutoBidsByAuction(auctionId);
        int[] highestEligibleAutoBid = null;
        double targetBidAmount = 0;

        double nextRequiredBid = auction.getCurrentPrice() + auction.getMinIncrement();

        // Tìm kiếm xem trong danh sách ai có cấu hình MaxBid hợp lệ và cao nhất lúc này
        for (int[] autoBid : autoBids) {
            int bidderId = autoBid[0];
            double maxBid = autoBid[1] / 100.0; // Giả định DB của bạn lưu dạng cents/nhân 100

            if (bidderId == lastBidderId) continue;

            if (maxBid >= nextRequiredBid) {
                if (highestEligibleAutoBid == null || maxBid > (highestEligibleAutoBid[1] / 100.0)) {
                    highestEligibleAutoBid = autoBid;
                    targetBidAmount = nextRequiredBid;
                }
            }
        }

        // Nếu tìm thấy Bot Auto-bid đủ điều kiện, tiến hành đặt giá tự động
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

                ClientHandler.pushBidUpdate(auctionId, targetBidAmount, "Tự động (User: " + botUsername + ")");

                // 🔄 ĐỆ QUY ĐUỔI VÒNG: Gọi lại chính nó để các cấu hình Auto-bid khác có cơ hội phản đòn
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
            // Giải phóng bộ nhớ map khóa khi phiên đóng
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
            Account account = accountDAO.getAccountById(Integer.parseInt(userId));
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

    public Map<String, Integer> getBidHistoryStats(int userId) {
        Map<String, Integer> stats = new java.util.HashMap<>();
        try {
            int total = bidDAO.countBidsByUser(userId);
            int won   = auctionDAO.countWonAuctions(userId);
            stats.put("total", total);
            stats.put("won",   won);
            stats.put("lost",  Math.max(0, total - won));
        } catch (Exception e) {
            stats.put("total", 0); stats.put("won", 0); stats.put("lost", 0);
        }
        return stats;
    }

    public boolean setAutoBid(int auctionId, String userId, double maxBid, double increment) {
        try {
            return registerAutoBid(auctionId, Integer.parseInt(userId), maxBid);
        } catch (Exception e) {
            return false;
        }
    }

    public Map<String, Integer> getDashboardStats(int userId) {
        Map<String, Integer> stats = new java.util.HashMap<>();
        try {
            int ongoing = getAuctionsByBidder(userId).size();
            int won     = auctionDAO.countWonAuctions(userId);
            stats.put("ongoing", ongoing);
            stats.put("won",     won);
        } catch (Exception e) {
            stats.put("ongoing", 0); stats.put("won", 0);
        }
        return stats;
    }

    public List<Auction> getAuctionsByStatus_wrapper(Auction.AuctionStatus status) {
        return auctionDAO.getAuctionsByStatus(status);
    }
}