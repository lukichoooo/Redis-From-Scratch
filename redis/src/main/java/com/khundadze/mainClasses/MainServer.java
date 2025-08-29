package com.khundadze.mainClasses;

import com.khundadze.server_client.NioServer;

public class MainServer {
    public static void main(String[] args) {
        new NioServer().start(5230);
    }
}