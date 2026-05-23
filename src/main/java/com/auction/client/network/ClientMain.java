package com.auction.client.network;


import com.auction.shared.network.Message;
import com.auction.shared.network.MessageType;

public class ClientMain {

    public static void main(String[] args) {

        try {

            ClientSocket client =
                    new ClientSocket();

            client.connect();

            // gửi message

            Message msg =
                    new Message(
                            MessageType.HELLO,
                            "Hello server"
                    );

            client.send(msg);

            // nhận phản hồi

            Message response =
                    client.receive();

            System.out.println(
                    "Client received: "
                            + response.getData());

        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}