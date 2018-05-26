package client;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class Presenter {

    private SwingView view = new SwingView();
    private PrintWriter output;

    public Presenter() {
        view.getConnectButton().addActionListener(e -> {
            String host = view.getHostTextField().getText();
            String port = view.getPortTextField().getText();
            String name = view.getNameTextField().getText();
            if (!host.isEmpty() && !port.isEmpty() && !name.isEmpty()) {
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

                            out.println("Nickname:" + name);
                            output = out;
                            for (String fromServer; (fromServer = in.readLine()) != null;) {
                                publish(fromServer);
                            }
                        }
                        return null;
                    }

                    @Override
                    protected void process(List<String> messages) {
                        if (messages != null && !messages.isEmpty()) {
                            view.getStatusLabel().setText(String.format(
                                    "%s successfully connected to port %s on host %s",
                                    name, port, host));
                            for (String message : messages) {
                                view.getInputTextArea().append(message + "\n");
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
            if (!view.getOutputTextField().getText().isEmpty()) {
                output.println(view.getOutputTextField().getText());
            }
        });
    }
}