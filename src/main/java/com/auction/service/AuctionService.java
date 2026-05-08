package com.auction.service;

import com.auction.dao.AuctionDAO;
import com.auction.dao.BidDAO;
import com.auction.dao.ItemDAO;
import com.auction.model.*;
import com.auction.observer.AuctionObserver;
import com.auction.observer.AuctionSubject;

import java.util.ArrayList;
import java.util.List;

public class AuctionService implements AuctionSubject {

    private final AuctionDAO auctionDAO = new AuctionDAO();
    private final BidDAO bidDAO = new BidDAO();
    private final ItemDAO itemDAO = new ItemDAO();

    // Observer list
    private final List<AuctionObserver> observers = new ArrayList<>();

    @Override
    public void addObserver(AuctionObserver observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(AuctionObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(int auctionId, double newPrice) {
        for (AuctionObserver observer : observers) {
            observer.onBidPlaced(auctionId, newPrice);
        }
    }

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

    // 2. Đặt giá
    public boolean placeBid(int auctionId, double amount, Account account) {
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

        boolean bidSaved = bidDAO.insertBid(bid);
        if (!bidSaved) return false;

        boolean priceUpdated = auctionDAO.updateCurrentPrice(auctionId, amount, Integer.parseInt(account.getId()));
        if (!priceUpdated) return false;

        // Notify tất cả observer khi bid thành công
        notifyObservers(auctionId, amount);

        System.out.println("Đặt giá thành công: " + amount);
        return true;
    }

    // 3. Đóng phiên đấu giá
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

    // 7. Lấy lịch sử bid
    public List<BidTransaction> getBidHistory(int auctionId) {
        return bidDAO.getBidsByAuction(auctionId);
    }
}