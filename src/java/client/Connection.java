package client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.ExecutionException;

public final class Connection extends SwingWorker<Void, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Connection.class);
    private static String host = "localhost";
    private static final int PORT = 9009;
    private static PrintWriter out;
    private static Presenter presenter;

    public static void setHost(String host) {
        Connection.host = host;
        LOGGER.info("Specified host: {}", host);
    }

    public static void setPresenter(Presenter presenter) {
        Connection.presenter = presenter;
    }

    @Override
    protected Void doInBackground() throws IOException {
        LOGGER.debug("Trying to connect to port {} on host {}", PORT, host);
        try (Socket socket = new Socket(host, PORT);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(
                             socket.getInputStream(), "UTF-8"));
             PrintWriter out = new PrintWriter(new BufferedWriter(
                     new OutputStreamWriter(
                             socket.getOutputStream(), "UTF-8")), true)) {

            LOGGER.info("Connection has been established to port {} on host {}", PORT, host);
            Connection.out = out;
            send(Command.Output.TIMEZONE, ZoneId.systemDefault().toString());

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

    public static void send(Command.Output command, String... output) {
        if (out != null) {
            out.println(command + Command.DELIMITER + String.join(Command.DELIMITER, output));
            LOGGER.trace("From client: {}", command.name());
        }
    }

    @Override
    protected void done() {
        try {
            get();
        } catch (InterruptedException | ExecutionException e) {
            presenter.stop();
            LOGGER.error("Failed to connect to server", e);
        } finally {
            out = null;
        }
    }
}