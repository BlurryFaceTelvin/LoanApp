package com.example.serverlibraty;

import com.africastalking.AfricasTalking;
import com.africastalking.Server;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import spark.Spark;

import static spark.Spark.port;
import static spark.Spark.post;
import static spark.route.HttpMethod.get;

public class PaymentServer {

    private static final int RPC_PORT = 35897;
    private static final int HTTP_PORT = 30001;
    public static void main(String[] args) {
        System.out.print("Starting................");
        AfricasTalking.initialize("loans", "dee8b36900884e96bdde7e9fc09df94fc7d7ced66324c454bb76c8a17f420803");
        Server server = new Server();
        try {
            server.startInsecure(RPC_PORT);

        } catch (IOException e) {
            e.printStackTrace();
        }

        //set our port
        port(HTTP_PORT);
        HashMap<String,String> transactions = new HashMap<>();
        Spark.get("/transactionLoan/status",(request, response) -> {

            return transactions.get("status");
        });
        post("/notify",(request, response) -> {

            Gson gson = new Gson();
            AfricasTalkingNotification notification = gson.fromJson(request.body(),AfricasTalkingNotification.class);
            transactions.put("status",notification.status);
            System.out.println(request.body());
            return "OK";
        });


    }
    //model for data
    static class AfricasTalkingNotification{
        String status;
    }

}


