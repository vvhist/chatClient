package client.Login;

import client.Chat.ChatView;
import client.Connection;
import client.Presenter;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Arrays;

public final class LoginPresenter implements Presenter {

    private final LoginView view;
    private String enteredUsername;
    private char[] enteredPassword;

    LoginPresenter(LoginView view) {
        this.view = view;
        new Connection().execute();
        Connection.setPresenter(this);
    }

    public void logIn(String username, char[] password) {
        if (areInvalid(username, password)) return;

        view.enableComponents(view.$$$getRootComponent$$$(), false);
        enteredUsername = username;
        enteredPassword = password;
        Connection.send("hash " + username);
    }

    public void register(String username, char[] password) {
        if (areInvalid(username, password)) return;

        view.enableComponents(view.$$$getRootComponent$$$(), false);
        enteredUsername = username;
        String hashedPassword = BCrypt.hashpw(new String(password), BCrypt.gensalt());
        Arrays.fill(password, '0');
        Connection.send("reg " + username + " " + hashedPassword);
    }

    private boolean areInvalid(String username, char[] password) {
        if (username.isEmpty() || password.length == 0) {
            view.displayWarning("Please fill in all the fields");
            return true;
        }
        if (username.contains(" ")) {
            view.displayWarning("A username cannot contain spaces");
            return true;
        }
        return false;
    }

    @Override
    public void processInput(String input) {
        if (input.equals("allowReg")) {
            openChatWindow();
        } else if (input.startsWith("hash")) {
            String hash = input.substring(input.indexOf(' ') + 1);
            if (BCrypt.checkpw(new String(enteredPassword), hash)) {
                openChatWindow();
                Connection.send("log " + enteredUsername);
            } else {
                view.enableComponents(view.$$$getRootComponent$$$(), true);
                view.displayWarning("Incorrect username or password");
            }
            Arrays.fill(enteredPassword, '0');
        } else if (input.equals("denyLog")) {
            view.enableComponents(view.$$$getRootComponent$$$(), true);
            view.displayWarning("Incorrect username or password");
        } else if (input.equals("denyReg")) {
            view.enableComponents(view.$$$getRootComponent$$$(), true);
            view.displayWarning("This name is already taken");
        }
    }

    private void openChatWindow() {
        view.close();
        new ChatView(enteredUsername);
    }

    @Override
    public void stop() {
        String warning = "Failed to connect to server";
        view.enableComponents(view.$$$getRootComponent$$$(), false);
        view.displayWarning(warning);
    }

    @Override
    public void closeApp() {
        Connection.send("exit");
        System.exit(0);
    }
}