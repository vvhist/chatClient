package client;

import javax.swing.*;

import java.awt.event.*;
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

    public Presenter() {
        new ConnectionHandler().execute();
        loginForm = new Login(new CloseOperation());

        loginForm.getLoginButton().addActionListener(e -> {
            String authData = getAuthData();
            if (authData.isEmpty()) return;
            output.println("/log/" + authData);
        });

        loginForm.getRegisterButton().addActionListener(e -> {
            String authData = getAuthData();
            if (authData.isEmpty()) return;
            output.println("/reg/" + authData);
        });
    }

    private String getAuthData() {
        String username = loginForm.getUsernameField().getText();
        char[] password = loginForm.getPasswordField().getPassword();

        if (username.isEmpty() || password.length == 0) return "";
        if (username.contains("/") || username.contains(":") || username.contains(" ")) {
            loginForm.displayWarning("A username cannot contain /, : or spaces");
            return "";
        }
        StringBuilder builder = new StringBuilder(username + "/");
        for (char c : password) {
            builder.append(c);
        }
        Arrays.fill(password, '0');
        return builder.toString();
    }

    private void addChatListeners() {
        chatForm.getSendButton().addActionListener(e -> {
            String outputText = chatForm.getOutputTextField().getText();
            if (outputText.isEmpty()) return;

            String contact = chatForm.getTabbedPane().getTitleAt(
                    chatForm.getTabbedPane().getSelectedIndex());
            output.println(contact + " " + outputText);
            chatForm.getOutputTextField().setText("");
        });

        chatForm.getOutputTextField().addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    chatForm.getSendButton().doClick();
                }
            }
        });
    }

    private void addNewContact(String name) {
        JTextArea textArea = chatForm.addTab(name);
        dialogs.put(name, textArea);
    }


    private final class CloseOperation extends WindowAdapter {

        @Override
        public void windowClosing(WindowEvent e) {
            if (output != null) {
                output.println("/exit");
            }
            System.exit(0);
        }
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
                if (input.equals("/pass")) {
                    loginForm.close();
                    loginForm = null;
                    chatForm = new Chat(new CloseOperation());
                    addChatListeners();
                    addNewContact("Server");
                } else if (input.equals("/deny")) {
                    loginForm.displayWarning("Incorrect username or password");
                } else if (input.equals("/denyName")) {
                    loginForm.displayWarning("This name is already taken");
                } else if (input.startsWith("/newDialog ")) {
                    String contact = input.replace("/newDialog ", "");
                    if (!dialogs.containsKey(contact)) {
                        addNewContact(contact);
                    } else {
                        dialogs.get("Server").append(
                                "Conversation with " + contact + " already exists." + "\n");
                    }
                } else {
                    String contact = input.substring(0, input.indexOf(' '));
                    String message = input.substring(input.indexOf(' ') + 1);
                    if (!dialogs.containsKey(contact)) {
                        addNewContact(contact);
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
                    chatForm.displayStatus(warning);
                }
                e.printStackTrace();
            }
        }
    }
}