package com.khundadze;

import java.util.Scanner;

import com.khundadze.server_client.NioClient;

public class MainClient {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        new NioClient().start(5230, scanner);
        scanner.close();
    }
}
