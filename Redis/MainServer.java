import server.NioServer;

public class MainServer {
    public static void main(String[] args) {
        new NioServer().start(8080);
    }
}
