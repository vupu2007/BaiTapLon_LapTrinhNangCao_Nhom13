package com.auction.client.network;

import com.auction.shared.network.Message;

import java.net.Socket;
import java.io.*;

public class ClientSocket {

    private Socket socket;

    private ObjectInputStream in;
    private ObjectOutputStream out;

    public void connect() throws Exception {

        socket = new Socket(
                "localhost",
                9999
        );

        System.out.println(
                "Connected to server");

        // Output trước Input

        out = new ObjectOutputStream(
                socket.getOutputStream());

        in = new ObjectInputStream(
                socket.getInputStream());
    }

    public void send(Message msg)
            throws Exception {

        out.writeObject(msg);

        out.flush();
    }

    public Message receive()
            throws Exception {

        return (Message) in.readObject();
    }
}