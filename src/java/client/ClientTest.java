package client;

public class ClientTest {

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: java chat/Client <host name> <port number> <nickname>");
            System.exit(1);
        }
        for (int i = 0; i <= args[2].length(); i++) {
            new Thread().start();
        }
        new Thread(() -> {
            System.out.println("ClientThread: " + Thread.currentThread().getName());
            Client.main(args);
        }).start();
    }
}