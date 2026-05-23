package com.auction.client.network;

import com.auction.shared.network.Request;
import com.auction.shared.network.Response;
import java.io.*;
import java.net.Socket;

public class ClientSocket {
    // Biến static duy nhất lưu trữ kết nối
    private static ClientSocket instance;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    // Thay đổi Constructor thành public hoặc private tùy bạn,
    // nhưng giữ nguyên để không lỗi code cũ của bạn
    public ClientSocket() {}

    // Hàm getInstance() để các Controller (Register, Login) dùng chung 1 kết nối
    public static synchronized ClientSocket getInstance() {
        if (instance == null) {
            instance = new ClientSocket();
        }
        return instance;
    }

    // Hàm kết nối đến Server
    public void connect() throws Exception {
        if (this.socket == null || this.socket.isClosed()) {
            // Thay "localhost" và port 12345 bằng cấu hình thực tế của Server bạn
            this.socket = new Socket("localhost", 12345);
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());
            System.out.println("Kết nối đến Server thành công!");
        }
    }

    // Gửi Request (Đã sửa từ Message thành Request)
    public void send(Request request) throws Exception {
        if (out != null) {
            out.writeObject(request);
            out.flush();
        }
    }

    // Nhận Response (Đã sửa từ Message thành Response)
    public Response receive() throws Exception {
        if (in != null) {
            return (Response) in.readObject();
        }
        return null;
    }

    // Hàm tiện ích kết hợp gửi và nhận cùng lúc (Dùng cho RegisterController)
    public Response sendRequest(Request request) throws Exception {
        this.connect(); // Tự động kết nối nếu chưa kết nối
        this.send(request);
        return this.receive();
    }
}