import java.util.Scanner;

public class MainClient {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        new NioClient().start(5230, scanner);
        scanner.close();
    }
}
