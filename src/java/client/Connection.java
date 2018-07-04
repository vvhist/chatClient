package client;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.ExecutionException;

public final class Connection extends SwingWorker<Void, String> {

    private static final String HOST = "localhost";
    private static final int PORT = 9009;
    private static PrintWriter out;
    private static Presenter presenter;

    public static void setPresenter(Presenter presenter) {
        Connection.presenter = presenter;
    }

    @Override
    protected Void doInBackground() throws IOException {
        try (Socket socket = new Socket(HOST, PORT);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(
                             socket.getInputStream(), "UTF-8"));
             PrintWriter out = new PrintWriter(new BufferedWriter(
                     new OutputStreamWriter(
                             socket.getOutputStream(), "UTF-8")), true)) {

            out.println("zone " + ZoneId.systemDefault());
            Connection.out = out;

            for (String fromServer; (fromServer = in.readLine()) != null; ) {
                publish(fromServer);
            }
        }
        return null;
    }

    @Override
    protected void process(List<String> inputs) {
        inputs.forEach(presenter::processInput);
    }

    public static void send(String output) {
        if (out != null) {
            out.println(output);
        }
    }

    @Override
    protected void done() {
        try {
            get();
        } catch (InterruptedException | ExecutionException e) {
            presenter.stop();
            e.printStackTrace();
        } finally {
            out = null;
        }
    }
}