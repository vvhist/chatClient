package client;

import client.Login.LoginView;

import javax.swing.*;

public final class Application {

    public static void main(String[] args) {
        if (args.length == 1) {
            Connection.setHost(args[0]);
        }

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                    | UnsupportedLookAndFeelException e) {
                e.printStackTrace();
            }
            new LoginView();
        });
    }
}