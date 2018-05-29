package client;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class Presenter {

    private SwingView view = new SwingView();

    private List<String> tabs = new ArrayList<>();
    private Map<String, JTextArea> dialogs = new HashMap<>();

    private PrintWriter output;

    public Presenter() {
        addNewContact("Server");

        view.getConnectButton().addActionListener(e -> {
            String host = view.getHostTextField().getText();
            String port = view.getPortTextField().getText();
            String name = view.getNameTextField().getText();
            if (!host.isEmpty() &&!port.isEmpty() && !name.isEmpty()
                    && !name.equals("Server")
                    && !name.contains(":")
                    && !name.contains(" ")) {
                view.getStatusLabel().setText("Trying to establish connection...");

                new SwingWorker<Void, String>() {

                    @Override
                    protected Void doInBackground() throws IOException {
                        try (Socket socket = new Socket(host, Integer.parseInt(port));
                             BufferedReader in = new BufferedReader(
                                     new InputStreamReader(
                                             socket.getInputStream(),  "UTF-8"));
                             PrintWriter out = new PrintWriter(new BufferedWriter(
                                     new OutputStreamWriter(
                                             socket.getOutputStream(), "UTF-8")), true)) {

                            out.println("name " + name);
                            output = out;
                            for (String fromServer; (fromServer = in.readLine()) != null;) {
                                publish(fromServer);
                            }
                        }
                        return null;
                    }

                    @Override
                    protected void process(List<String> inputs) {
                        if (inputs != null && !inputs.isEmpty()) {
                            view.getStatusLabel().setText(name + " is online");
                            for (String input : inputs) {
                                if (input.startsWith("newChat ")) {
                                    String interlocutorName = input.replace("newChat ", "");
                                    if (!dialogs.containsKey(interlocutorName)) {
                                        addNewContact(interlocutorName);
                                    } else {
                                        dialogs.get("Server").append("Conversation with " + interlocutorName + " already exists." + "\n");
                                    }
                                } else {
                                    String interlocutorName = input.substring(0, input.indexOf(' '));
                                    String message = input.substring(input.indexOf(' ') + 1);
                                    if (!dialogs.containsKey(interlocutorName)) {
                                        addNewContact(interlocutorName);
                                    }
                                    dialogs.get(interlocutorName).append(message + "\n");
                                }
                            }
                        }
                    }

                    @Override
                    protected void done() {
                        try {
                            get();
                        } catch (InterruptedException | ExecutionException e) {
                            view.getStatusLabel().setText(e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }.execute();
            }
        });

        view.getSendButton().addActionListener(e -> {
            String outputText = view.getOutputTextField().getText();
            if (!outputText.isEmpty()) {
                String interlocutorName = tabs.get(view.getTabbedPane().getSelectedIndex());
                output.println(interlocutorName + " " + outputText);
                view.getOutputTextField().setText("");
            }
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
        tabs.add(name);
        dialogs.put(name, textArea);
    }
}