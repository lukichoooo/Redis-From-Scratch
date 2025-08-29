package com.khundadze.server_client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NioClient {

    private static final Logger logger = Logger.getLogger(NioClient.class.getName());

    public void start(final int portNumber, final Scanner scanner) {
        try (SocketChannel clientChannel = SocketChannel.open()) {

            clientChannel.connect(new InetSocketAddress("localhost", portNumber));
            logger.log(Level.INFO, "Client connected to port {0}", portNumber);

            while (true) {
                // 1️⃣ Read user input
                System.out.print("> ");
                String message = scanner.nextLine().trim();
                if (message.equalsIgnoreCase("exit"))
                    break;

                // 2️⃣ Send to server
                ByteBuffer writeBuffer = ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8));
                clientChannel.write(writeBuffer);

                // 3️⃣ Read response from server
                ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                int bytesRead = clientChannel.read(readBuffer);
                if (bytesRead > 0) {
                    readBuffer.flip();
                    byte[] data = new byte[readBuffer.limit()];
                    readBuffer.get(data);
                    String response = new String(data, StandardCharsets.UTF_8);
                    System.out.println("Server: " + response);
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
