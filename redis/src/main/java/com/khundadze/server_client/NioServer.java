package com.khundadze.server_client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.khundadze.data_structures.ZSet;

import com.khundadze.model.*;

// TODO: update server to use ZSet

public class NioServer {

    private static final Logger logger = Logger.getLogger(NioServer.class.getName());

    HashSet<SocketChannel> clients = null;

    ZSet<Object> zs;

    public NioServer() {
        clients = new HashSet<>();
        zs = new ZSet<>();
    }

    public void start(final int portNumber) {
        try (ServerSocketChannel serverChannel = ServerSocketChannel.open();
                Selector selector = Selector.open()) {

            // Bind server and configure non-blocking mode
            serverChannel.bind(new InetSocketAddress(portNumber));
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            logger.log(Level.INFO, "Server started on port {0}", portNumber);

            while (true) {
                if (selector.select() == 0)
                    continue; // No ready channels

                Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();

                    // 1️⃣ Accept new clients
                    if (key.isAcceptable() && key.channel() instanceof ServerSocketChannel ssc) {
                        SocketChannel client = ssc.accept();
                        client.configureBlocking(false);
                        client.register(selector, SelectionKey.OP_READ);
                        clients.add(client);

                        logger.log(Level.INFO, "Client connected: {0}", client.getRemoteAddress());
                    }

                    // 2️⃣ Handle readable client
                    if (key.isReadable() && key.channel() instanceof SocketChannel client) {
                        handleClient(client);
                    }

                    keyIterator.remove(); // Remove the processed key
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            clients.forEach(client -> {
                try {
                    client.close();
                } catch (IOException e) {
                    throw new RuntimeException("Failed to close client channel");
                }
            });
        }
    }

    // ------------------------ Helper methods ------------------------

    private void handleClient(SocketChannel client) {
        try {
            RequestDto request = readRequest(client);

            if (request != null) {
                ResponseDto response = handleRequest(request);

                sendResponse(client, response);
            }
        } catch (IOException e) {
            disconnectClient(client);
        }
    }

    private void disconnectClient(SocketChannel client) {
        try {
            logger.log(Level.INFO, "Client disconnected: {0}", client.getRemoteAddress());
            clients.remove(client);
            client.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private RequestDto readRequest(SocketChannel client) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int bytesRead = client.read(buffer);

        if (bytesRead <= 0)
            return null; // nothing or disconnected

        buffer.flip();
        byte[] data = new byte[buffer.limit()];
        buffer.get(data);

        String message = new String(data).trim();
        String[] parts = message.split(" ", 4);

        Command command = Command.valueOf(parts[0]);
        String name = parts.length > 1 ? parts[1] : null;
        Object value = parts.length > 2 ? parts[2] : null;
        Double score = parts.length > 3 ? Double.parseDouble(parts[3]) : 1.0;

        return new RequestDto(command, name, value, score);
    }

    private ResponseDto handleRequest(RequestDto request) {
        switch (request.command()) {
            case SET -> {
                zs.add(request.name(), request.score(), request.value());
                return new ResponseDto(ServerType.SERVER_STRING, "OK");
            }
            case DEL -> {
                zs.remove(request.name());
                return new ResponseDto(ServerType.SERVER_STRING, "OK");
            }
            case GET -> {
                return new ResponseDto(ServerType.SERVER_STRING, zs.get(request.name()));
            }
            case KEYS -> {
                return new ResponseDto(ServerType.SERVER_ARRAY, Arrays.toString(zs.keySet()));
            }
            default -> {
                return new ResponseDto(ServerType.SERVER_ERROR, "Unknown command");
            }
        }
    }

    private void sendResponse(SocketChannel client, ResponseDto response) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(response.value().toString().getBytes());
        client.write(buffer); // keep connection open
    }

}
