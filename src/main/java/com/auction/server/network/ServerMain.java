package com.auction.server.network;

import java.net.ServerSocket;
import java.net.Socket;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerMain {

    // thread pool 10 luồng

    private static final ExecutorService pool =
            Executors.newFixedThreadPool(10);

    public static void main(String[] args) {

        try {

            ServerSocket serverSocket =
                    new ServerSocket(9999);

            System.out.println("Server started");

            while(true) {

                Socket socket =
                        serverSocket.accept();

                System.out.println(
                        "Client connected");

                ClientHandler handler =
                        new ClientHandler(socket);

                // dùng thread pool- đâyr các lệnh vào queue

                pool.execute(handler);
            }

        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}