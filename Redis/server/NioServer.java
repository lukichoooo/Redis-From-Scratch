package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NioServer {

    private static final Logger logger = Logger.getLogger(NioServer.class.getName());

    public void start(final int portNumber) {
        HashSet<SocketChannel> clients = new HashSet<>();

        try (var serverSocketChannel = ServerSocketChannel.open();
                var selector = Selector.open()) {

            serverSocketChannel.bind(new InetSocketAddress(portNumber));
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            logger.log(Level.INFO, "Server started on port {0}", portNumber);

            while (true) {
                if (selector.select() == 0)
                    continue;

                Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();

                    if (key.isAcceptable() && key.channel() instanceof ServerSocketChannel serverChannel) {
                        SocketChannel client = serverChannel.accept();
                        client.configureBlocking(false);
                        client.register(selector, SelectionKey.OP_READ);
                        clients.add(client);

                        logger.log(Level.INFO, "Client connected: {0}", client.getRemoteAddress());
                    }

                    if (key.isReadable() && key.channel() instanceof SocketChannel client) {
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        int bytesRead = client.read(buffer);
                        if (bytesRead == -1) {
                            clients.remove(client);

                            logger.log(Level.INFO, "Client disconnected: {0}", client.getRemoteAddress());

                            client.close();
                        } else {
                            buffer.flip();
                            byte[] data = new byte[buffer.limit()];
                            buffer.get(data);
                            String message = new String(data);

                            logger.log(Level.INFO, "Received from client: {0}", message);
                        }
                    }

                    keyIterator.remove(); // Remove processed key
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            clients.forEach(client -> {
                try {
                    client.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
