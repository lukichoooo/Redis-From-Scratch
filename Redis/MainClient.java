import client.NioClient;
import java.util.Scanner;

public class MainClient {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        new NioClient().start(8080, scanner);
        scanner.close();
    }
}
