package client;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: java chat/Client <host name> <port number> <nickname>");
            System.exit(1);
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String nickname = args[2];

        try (Socket socket = new Socket(host, port);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(
                             socket.getInputStream(),  "UTF-8"));
             PrintWriter out = new PrintWriter(new BufferedWriter(
                     new OutputStreamWriter(
                             socket.getOutputStream(), "UTF-8")), true);
             BufferedReader userIn = new BufferedReader(
                     new InputStreamReader(
                             System.in,                "UTF-8"))) {

            new Thread(() -> {
                try {
                    out.println("Nickname:" + nickname);
                    for (String userInput; (userInput = userIn.readLine()) != null; ) {
                        out.println(userInput);
                        if (userInput.equals("bye")) {
                            break;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            for (String fromServer; (fromServer = in.readLine()) != null; ) {
                System.out.println(fromServer);
            }
        } catch (UnknownHostException e) {
            System.err.println("Unknown host: " + host);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}