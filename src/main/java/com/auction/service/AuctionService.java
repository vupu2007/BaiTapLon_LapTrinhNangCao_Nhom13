package com.auction.service;

import com.auction.dao.AuctionDAO;
import com.auction.dao.BidDAO;
import com.auction.dao.ItemDAO;
import com.auction.model.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionService {

    private final AuctionDAO auctionDAO = new AuctionDAO();
    private final BidDAO bidDAO = new BidDAO();
    private final ItemDAO itemDAO = new ItemDAO();
    private final ReentrantLock lock = new ReentrantLock();


    // 1. Seller tạo phiên đấu giá mới
    public boolean createAuction(Auction auction) {
        // Kiểm tra item tồn tại và đang AVAILABLE
        Item item = itemDAO.getItemById(auction.getItemId());
        if (item == null) {
            System.err.println("Sản phẩm không tồn tại!");
            return false;
        }
        if (!"AVAILABLE".equals(item.getStatus())) {
            System.err.println("Sản phẩm không ở trạng thái AVAILABLE!");
            return false;
        }

        // Kiểm tra thời gian hợp lệ
        if (auction.getEndTime().isBefore(auction.getStartTime())) {
            System.err.println("Thời gian kết thúc phải sau thời gian bắt đầu!");
            return false;
        }

        // Tạo phiên và cập nhật trạng thái item
        boolean created = auctionDAO.insertAuction(auction);
        if (created) {
            itemDAO.updateStatus(auction.getItemId(), "IN_AUCTION");
            System.out.println("Tạo phiên đấu giá thành công!");
        }
        return created;
    }

    // 2. Đặt giá — đây là chức năng cốt lõi
    public boolean placeBid(int auctionId, double amount, Account account) {
        // Chỉ User (Bidder/Seller) mới được đặt giá, không phải Admin
        lock.lock();
        try {
            if (!(account instanceof User)) {
                System.err.println("Admin không được phép đặt giá!");
                return false;
            }

            User user = (User) account;

            // Lấy thông tin phiên đấu giá
            Auction auction = auctionDAO.getAuctionById(auctionId);
            if (auction == null) {
                System.err.println("Phiên đấu giá không tồn tại!");
                return false;
            }

            // Kiểm tra phiên còn đang chạy không
            if (!auction.isActive()) {
                System.err.println("Phiên đấu giá đã kết thúc hoặc chưa bắt đầu!");
                return false;
            }

            // Không được tự đấu giá sản phẩm của chính mình
            if (auction.getSellerId() == Integer.parseInt(account.getId())) {
                System.err.println("Không thể đấu giá sản phẩm của chính mình!");
                return false;
            }

            // Kiểm tra giá phải cao hơn giá hiện tại + bước giá tối thiểu
            double minValidBid = auction.getCurrentPrice() + auction.getMinIncrement();
            if (amount < minValidBid) {
                System.err.println("Giá đặt phải ít nhất là: " + minValidBid);
                return false;
            }

            // Kiểm tra số dư ví
            if (user.getBalance() < amount) {
                System.err.println("Số dư không đủ!");
                return false;
            }

            // Lưu bid vào DB
            BidTransaction bid = new BidTransaction();
            bid.setAuctionId(auctionId);
            bid.setBidderId(Integer.parseInt(account.getId()));
            bid.setBidAmount(amount);

            boolean bidSaved = bidDAO.insertBid(bid);
            if (!bidSaved) return false;

            // Cập nhật giá hiện tại trong phiên đấu giá
            boolean priceUpdated = auctionDAO.updateCurrentPrice(auctionId, amount, Integer.parseInt(account.getId()));
            if (!priceUpdated) return false;

            System.out.println("Đặt giá thành công: " + amount);

            // 2. THÊM VÀO ĐÂY: Phát tín hiệu cho các màn hình (Observers) cập nhật lại giá
            // Giả sử bạn truyền giá mới và tên người đặt
            auction.notifyObservers();
            return true;
        } finally {
            lock.unlock();
        }
    }



    // 3. Đóng phiên đấu giá (khi hết giờ hoặc Admin kết thúc)
    public boolean closeAuction(int auctionId) {
        Auction auction = auctionDAO.getAuctionById(auctionId);
        if (auction == null) {
            System.err.println("Phiên đấu giá không tồn tại!");
            return false;
        }

        // Chuyển trạng thái sang FINISHED
        boolean closed = auctionDAO.updateStatus(auctionId, Auction.AuctionStatus.FINISHED);
        if (closed) {
            // Cập nhật trạng thái item: SOLD nếu có winner, AVAILABLE lại nếu không ai bid
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
}