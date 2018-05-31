package client;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class Presenter {

    private SwingView view = new SwingView();
    private Map<String, JTextArea> dialogs = new HashMap<>();
    private PrintWriter output;

    public Presenter() {
        new ConnectionHandler().execute();
        addNewContact("Server");
        view.displayStatus("Please log in");

        view.getLoginButton().addActionListener(e -> {
            String name = view.getNameTextField().getText();

            if (name.isEmpty() || name.equals("Server") || name.contains(":")
                                                        || name.contains(" ")) {
                return;
            }
            output.println("name " + name);
            view.displayStatus(name + " is online");
        });

        view.getSendButton().addActionListener(e -> {
            String outputText = view.getOutputTextField().getText();
            if (outputText.isEmpty()) return;

            String contact = view.getTabbedPane().getTitleAt(
                             view.getTabbedPane().getSelectedIndex());
            output.println(contact + " " + outputText);
            view.getOutputTextField().setText("");
        });

        view.getOutputTextField().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    view.getSendButton().doClick();
                }
            }
        });
    }

    private void addNewContact(String name) {
        JTextArea textArea = view.addTab(name);
        dialogs.put(name, textArea);
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
                if (input.startsWith("newChat ")) {
                    String contact = input.replace("newChat ", "");
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
                view.displayStatus(e.getMessage());
                e.printStackTrace();
            }
        }
    }
}