package com.auction.client.service;

import com.auction.client.network.ClientSocket;
import com.auction.client.util.CurrentAccount;
import com.auction.shared.model.Account;
import com.auction.shared.model.Auction;
import com.auction.shared.network.MessageType;
import com.auction.shared.network.Request;
import com.auction.shared.network.Response;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class AuctionService {
    private static final Logger LOGGER = Logger.getLogger(AuctionService.class.getName());
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Lớp DTO nay được đưa ra ngoài hoặc để public để Controller có thể đọc được
    public static class AuctionCardDto {
        public String name;
        public String price;
        public String time;
        public String image;
        public String description;
        public String sellerName;
        public String startTimeStr;
        public String endTimeStr;
        public Auction auction;
    }

    /**
     * Lấy danh sách các phiên đấu giá của tài khoản hiện tại từ Server qua Socket
     */
    public List<AuctionCardDto> getActiveAuctionsByCurrentBidder() throws Exception {
        List<AuctionCardDto> dataList = new ArrayList<>();

        Account currentAcc = CurrentAccount.getAccount();
        if (currentAcc == null) return dataList;

        int bidderIdInt = Integer.parseInt(currentAcc.getId());
        Request request = new Request(MessageType.GET_AUCTIONS_BY_BIDDER, bidderIdInt);
        Response response = ClientSocket.getInstance().sendRequest(request);

        if (response == null || !response.isSuccess() || !(response.getData() instanceof List<?> rawList)) {
            return dataList;
        }

        List<Auction> auctions = new ArrayList<>();
        for (Object obj : rawList) {
            if (obj instanceof Auction a) auctions.add(a);
        }
        if (auctions.isEmpty()) return dataList;

        LOGGER.info(() -> "Fetch thành công " + auctions.size() + " phiên đấu giá từ Server.");

        // Ép dữ liệu thô sang DTO
        for (Auction auction : auctions) {
            dataList.add(mapToCardDto(auction));
        }
        return dataList;
    }

    private AuctionCardDto mapToCardDto(Auction auction) {
        AuctionCardDto dto = new AuctionCardDto();
        dto.name = auction.getProductName() != null ? auction.getProductName() : "Sản phẩm #" + auction.getItemId();
        dto.image = auction.getImagePath();
        dto.description = auction.getDescription() != null ? auction.getDescription() : "Không có mô tả.";
        dto.sellerName = auction.getSellerName() != null ? auction.getSellerName() : "Người bán #" + auction.getSellerId();
        dto.startTimeStr = (auction.getStartTime() != null) ? auction.getStartTime().format(DATE_TIME_FORMATTER) : "--/--/---- --:--";
        dto.endTimeStr = (auction.getEndTime() != null) ? auction.getEndTime().format(DATE_TIME_FORMATTER) : "--/--/---- --:--";

        long minutes = Duration.between(LocalDateTime.now(), auction.getEndTime()).toMinutes();
        dto.time = (minutes > 0) ? minutes + " phút" : "Sắp kết thúc";
        dto.price = String.format("%,.0f VNĐ", auction.getCurrentPrice());
        dto.auction = auction;
        return dto;
    }
}