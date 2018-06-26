package client;

import org.mindrot.jbcrypt.BCrypt;

import javax.swing.*;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public final class Presenter {

    private Login loginForm;
    private Chat chatForm;

    private Map<String, JTextArea> dialogs = new HashMap<>();
    private PrintWriter output = null;
    private String enteredUsername;
    private char[] enteredPassword;

    public Presenter(Login loginForm) {
        this.loginForm = loginForm;
        new ConnectionHandler().execute();
    }

    public void logIn(String username, char[] password) {
        if (isInvalid(username) || password.length == 0) return;

        loginForm.enableComponents(loginForm.$$$getRootComponent$$$(), false);
        enteredUsername = username;
        enteredPassword = password;
        output.println("/log/" + username);
    }

    public void register(String username, char[] password) {
        if (isInvalid(username) || password.length == 0) return;

        loginForm.enableComponents(loginForm.$$$getRootComponent$$$(), false);
        enteredUsername = username;
        String hashedPassword = BCrypt.hashpw(new String(password), BCrypt.gensalt());
        Arrays.fill(password, '0');
        output.println("/reg/" + ZoneId.systemDefault() + " " + username + "/" + hashedPassword);
    }

    private boolean isInvalid(String username) {
        if (username.isEmpty()) return true;
        if (username.contains("/")) {
            loginForm.displayWarning("A username cannot contain /");
            return true;
        }
        return false;
    }

    public void searchNewContact(String contact) {
        if (contact.isEmpty()) return;

        if (dialogs.containsKey(contact)) {
            chatForm.displayWarning("Conversation with " + contact + " already exists.");
        } else {
            output.println("/add/" + contact);
        }
    }

    public void sendMessage(String contact, String text) {
        if (text.isEmpty()) return;

        output.println(contact + "/" + text);
    }

    public void closeApp() {
        if (output != null) {
            output.println("/exit");
        }
        System.exit(0);
    }


    private class ConnectionHandler extends SwingWorker<Void, String> {

        final String host = "localhost";
        final int port = 9009;

        String actualUsername = "";

        @Override
        protected Void doInBackground() throws IOException {
            try (Socket socket = new Socket(host, port);
                 BufferedReader in = new BufferedReader(
                         new InputStreamReader(
                                 socket.getInputStream(), "UTF-8"));
                 PrintWriter out = new PrintWriter(new BufferedWriter(
                         new OutputStreamWriter(
                                 socket.getOutputStream(), "UTF-8")), true)) {
                output = out;
                for (String fromServer; (fromServer = in.readLine()) != null; ) {
                    publish(fromServer);
                }
            }
            return null;
        }

        @Override
        protected void process(List<String> inputs) {
            for (String input : inputs) {
                if (input.startsWith("/")) {
                    if (input.equals("/allowReg")) {
                        actualUsername = enteredUsername;
                        loginForm.close();
                        loginForm = null;
                        chatForm = new Chat(Presenter.this);
                    } else if (input.startsWith("/hash/")) {
                        String hash = input.replace("/hash/", "");
                        if (BCrypt.checkpw(new String(enteredPassword), hash)) {
                            actualUsername = enteredUsername;
                            loginForm.close();
                            loginForm = null;
                            chatForm = new Chat(Presenter.this);
                            output.println("/match/" + enteredUsername + "/" + ZoneId.systemDefault());
                        } else {
                            loginForm.enableComponents(loginForm.$$$getRootComponent$$$(), true);
                            loginForm.displayWarning("Incorrect username or password");
                        }
                        Arrays.fill(enteredPassword, '0');
                    } else if (input.equals("/denyLog")) {
                        loginForm.enableComponents(loginForm.$$$getRootComponent$$$(), true);
                        loginForm.displayWarning("Incorrect username or password");
                    } else if (input.equals("/denyReg")) {
                        loginForm.enableComponents(loginForm.$$$getRootComponent$$$(), true);
                        loginForm.displayWarning("This name is already taken");
                    } else if (input.startsWith("/newDialog/")) {
                        String contact = input.replace("/newDialog/", "");
                        dialogs.put(contact, chatForm.getNewTab(contact));
                        chatForm.displayWarning(" ");
                    } else if (input.startsWith("/notFound/")) {
                        String contact = input.replace("/notFound/", "");
                        if (contact.length() > 30) {
                            contact = contact.substring(0, 30) + "...";
                        }
                        chatForm.displayWarning("User " + contact + " was not found");
                    }
                } else {
                    String[] messageParts = input.split("/", 4);
                    String sender    = messageParts[0];
                    String recipient = messageParts[1];
                    String timestamp = messageParts[2];
                    String text      = messageParts[3];

                    String contact = actualUsername.equals(sender) ? recipient : sender;
                    if (!dialogs.containsKey(contact)) {
                        dialogs.put(contact, chatForm.getNewTab(contact));
                    }
                    String time = LocalDateTime.parse(timestamp)
                            .toLocalTime()
                            .truncatedTo(ChronoUnit.SECONDS)
                            .format(DateTimeFormatter.ISO_LOCAL_TIME);
                    dialogs.get(contact).append(time + " " + sender + ": " + text + "\n");
                }
            }
        }

        @Override
        protected void done() {
            try {
                get();
            } catch (InterruptedException | ExecutionException e) {
                String warning = "Failed to connect to server";
                if (loginForm != null) {
                    loginForm.enableComponents(loginForm.$$$getRootComponent$$$(), false);
                    loginForm.displayWarning(warning);
                } else if (chatForm != null) {
                    chatForm.enableComponents(chatForm.$$$getRootComponent$$$(), false);
                    chatForm.displayWarning(warning);
                }
                e.printStackTrace();
            }
        }
    }
}