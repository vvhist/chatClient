package client;

import client.Login.LoginView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

public final class Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        LOGGER.info("Application started");
        if (args.length == 1) {
            Connection.setHost(args[0]);
        }

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                    | UnsupportedLookAndFeelException e) {
                LOGGER.error("Failed to load the specified LookAndFeel", e);
            }
            new LoginView();
            LOGGER.info("Login window has been opened");
        });
    }
}