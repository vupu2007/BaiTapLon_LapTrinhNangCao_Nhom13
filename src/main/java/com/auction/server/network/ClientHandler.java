package com.auction.server.network;

import com.auction.server.service.AccountService;
import com.auction.shared.model.Account;
import com.auction.shared.model.Admin;
import com.auction.shared.model.User;
import com.auction.shared.network.Request;
import com.auction.shared.network.Response;

import java.net.Socket;
import java.io.*;

public class ClientHandler implements Runnable {

    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private AccountService accountService = new AccountService();
    // Constructor chỉ nhận Socket, không nên khởi tạo Stream ở đây
    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            // QUAN TRỌNG: Khởi tạo Stream CHỈ 1 LẦN tại đây
            // Output trước Input để tránh lỗi block (deadlock) của Java Socket
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            while (true) {
                Request request = (Request) in.readObject();
                Response response = null;

                switch (request.getType()) {
                    case LOGIN:
                        // 1. Kiểm tra định dạng payload
                        if (request.getPayload() instanceof String[]) {
                            String[] credentials = (String[]) request.getPayload();

                            if (credentials.length == 2) {
                                String username = credentials[0];
                                String password = credentials[1];

                                // 2. Gọi hàm login từ AccountService. Hàm này trả về Account hoặc null.
                                Account loggedInAccount = accountService.login(username, password);

                                // 3. Dựa vào kết quả trả về để tạo Response
                                if (loggedInAccount != null) {
                                    // Đăng nhập thành công, ném luôn đối tượng xịn từ Database vào Response
                                    response = new Response(true, "Đăng nhập thành công!", loggedInAccount);
                                } else {
                                    // Đăng nhập thất bại (trả về null)
                                    response = new Response(false, "Sai tài khoản hoặc mật khẩu!", null);
                                }
                            } else {
                                response = new Response(false, "Dữ liệu đăng nhập không đủ!", null);
                            }
                        } else {
                            response = new Response(false, "Định dạng dữ liệu không hợp lệ!", null);
                        }
                        break;

                    case REGISTER:
                        // Bóc tách dữ liệu mảng String ngay tại ClientHandler
                        if (request.getPayload() instanceof String[]) {
                            String[] data = (String[]) request.getPayload();

                            // Đảm bảo có đủ 3 phần tử: username, password, email
                            if (data.length == 3) {
                                String username = data[0];
                                String password = data[1];
                                String email = data[2];

                                // Gọi thẳng vào hàm register cũ của bạn (trả về boolean)
                                boolean isSuccess = accountService.register(username, password, email);

                                if (isSuccess) {
                                    response = new Response(true, "Đăng ký tài khoản thành công!", null);
                                } else {
                                    response = new Response(false, "Đăng ký thất bại! Trùng tài khoản hoặc sai thông tin.", null);
                                }
                            } else {
                                response = new Response(false, "Dữ liệu đăng ký không đủ 3 trường thông tin!", null);
                            }
                        } else {
                            response = new Response(false, "Định dạng dữ liệu không hợp lệ!", null);
                        }
                        break;

                    case LOGOUT:
                        response = new Response(true, "Đã đăng xuất", null);
                        break;

                    default:
                        response = new Response(false, "Lệnh không hợp lệ", null);
                }

                out.writeObject(response);
                out.flush();
            }
        } catch (Exception e) {
            System.out.println("Client đã ngắt kết nối hoặc có lỗi mạng: " + e.getMessage());
        } finally {
            // Thêm khối finally để đóng tài nguyên sạch sẽ khi Client ngắt kết nối
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null) socket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}