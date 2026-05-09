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

import java.util.List;

public class AuctionService {

    private final AuctionDAO auctionDAO = new AuctionDAO();
    private final BidDAO     bidDAO     = new BidDAO();
    private final ItemDAO    itemDAO    = new ItemDAO();
    private final AccountDAO accountDAO = new AccountDAO();

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

        boolean created = auctionDAO.insertAuction(auction);
        if (created) {
            itemDAO.updateStatus(auction.getItemId(), "IN_AUCTION");
        }
        return created;
    }

    // 2. Đặt giá — synchronized để tránh race condition khi nhiều người bid cùng lúc
    public synchronized boolean placeBid(int auctionId, double amount, Account account) {
        if (!(account instanceof User)) {
            System.err.println("Admin không được phép đặt giá!");
            return false;
        }

        User user = (User) account;

        // Luôn đọc lại từ DB để có giá mới nhất, tránh lost update
        Auction auction = auctionDAO.getAuctionById(auctionId);
        if (auction == null || !auction.isActive()) {
            System.err.println("Phiên đấu giá không tồn tại hoặc đã kết thúc!");
            return false;
        }

        // Không cho tự đấu giá sản phẩm của mình
        if (auction.getSellerId() == Integer.parseInt(account.getId())) {
            System.err.println("Không thể đấu giá sản phẩm của chính mình!");
            return false;
        }

        // Kiểm tra giá đặt phải cao hơn giá hiện tại + bước giá tối thiểu
        double minValidBid = auction.getCurrentPrice() + auction.getMinIncrement();
        if (amount < minValidBid) {
            System.err.println("Giá đặt phải lớn hơn " + minValidBid);
            return false;
        }

        // Kiểm tra số dư ví
        if (user.getBalance() < amount) {
            System.err.println("Số dư không đủ!");
            return false;
        }

        int bidderId = Integer.parseInt(account.getId());

        // Gộp insertBid + updateCurrentPrice vào 1 transaction
        // → đảm bảo không có trạng thái nửa vời nếu một trong 2 lệnh lỗi
        BidTransaction bid = new BidTransaction();
        bid.setAuctionId(auctionId);
        bid.setBidderId(bidderId);
        bid.setBidAmount(amount);

        return auctionDAO.placeBidTransaction(bid, amount, bidderId);
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
            System.out.println("Phiên đấu giá đã kết thúc!");
        }
        return closed;
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