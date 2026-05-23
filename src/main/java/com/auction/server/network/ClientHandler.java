package com.auction.server.network;

import com.auction.shared.network.Message;
import com.auction.shared.network.MessageType;

import java.net.Socket;
import java.io.*;

public class ClientHandler implements Runnable {

    private Socket socket;

    private ObjectInputStream in;
    private ObjectOutputStream out;

    public ClientHandler(Socket socket)
            throws Exception {

        this.socket = socket;

        // QUAN TRỌNG:
        // Output trước Input

        out = new ObjectOutputStream(
                socket.getOutputStream());

        in = new ObjectInputStream(
                socket.getInputStream());
    }

    @Override
    public void run() {

        try {

            while(true) {

                Message msg =
                        (Message) in.readObject();

                System.out.println(
                        "Server received: "
                                + msg.getData());

                // phản hồi lại client

                Message response =
                        new Message(
                                MessageType.RESPONSE,
                                "Hello client"
                        );

                out.writeObject(response);

                out.flush();
            }

        } catch(Exception e) {

            System.out.println(
                    "Client disconnected");
        }
    }
}