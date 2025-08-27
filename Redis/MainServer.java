public class MainServer {
    public static void main(String[] args) {
        new NioServer().start(5230);
    }
}