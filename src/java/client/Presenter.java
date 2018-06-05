package client;

import javax.swing.*;

import java.io.*;
import java.net.Socket;
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

    public Presenter(Login loginForm) {
        this.loginForm = loginForm;
        new ConnectionHandler().execute();
    }

    public void logIn(String username, char[] password) {
        sendAuthData("/log/", username, password);
    }

    public void register(String username, char[] password) {
        sendAuthData("/reg/", username, password);
    }

    private void sendAuthData(String command, String username, char[] password) {
        if (username.isEmpty() || password.length == 0) return;
        if (username.contains("/")) {
            loginForm.displayWarning("A username cannot contain /");
            return;
        }
        StringBuilder builder = new StringBuilder(username + "/");
        for (char c : password) {
            builder.append(c);
        }
        Arrays.fill(password, '0');
        output.println(command + builder.toString());
    }

    public void searchNewContact(String contact) {
        if (contact.isEmpty()) return;

        if (dialogs.containsKey(contact)) {
            chatForm.displayWarning("Conversation with " + contact + " already exists.");
        } else {
            output.println("/add/" + contact);
        }
    }

    public void sendOutput(String contact, String text) {
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
                    if (input.equals("/pass")) {
                        loginForm.close();
                        loginForm = null;
                        chatForm = new Chat(Presenter.this);
                    } else if (input.equals("/deny")) {
                        loginForm.displayWarning("Incorrect username or password");
                    } else if (input.equals("/denyName")) {
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
                    String contact = input.substring(0, input.indexOf('/'));
                    String message = input.substring(input.indexOf('/') + 1);
                    if (!dialogs.containsKey(contact)) {
                        dialogs.put(contact, chatForm.getNewTab(contact));
                    }
                    dialogs.get(contact).append(message + "\n");
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