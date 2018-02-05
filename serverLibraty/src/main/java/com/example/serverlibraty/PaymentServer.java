package com.example.serverlibraty;

import com.africastalking.AfricasTalking;
import com.africastalking.Server;

import java.io.IOException;

public class PaymentServer {
    private static final int RPC_PORT = 35897;
    public static void main(String[] args){
        System.out.print("Starting................");
        AfricasTalking.initialize("loans","5141ba7d554070d1da45a82a4b2e6e4e7eef413014feee547c05fc415c5d0d80");
        Server server = new Server();

        try {
            server.startInsecure(RPC_PORT);
            while (true) {
                Thread.sleep(30000);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
