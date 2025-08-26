package client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NioClient {

    private static final Logger logger = Logger.getLogger(NioClient.class.getName());

    public void start(final int portNUmber, final Scanner scanner) {
        try (var serverChannel = SocketChannel.open();) {
            serverChannel.connect(new InetSocketAddress("localhost", portNUmber));

            logger.log(Level.INFO, "Client connected to port {0}", portNUmber);

            while (true) {
                String message = scanner.nextLine();
                if (message.equals("exit"))
                    break;
                ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
                serverChannel.write(buffer);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
