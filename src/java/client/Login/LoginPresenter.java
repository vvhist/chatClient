package client.Login;

import client.*;
import client.Chat.ChatView;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Arrays;

public final class LoginPresenter implements client.Presenter {

    private final View view;
    private String enteredUsername;
    private char[] enteredPassword;

    LoginPresenter(View view) {
        this.view = view;
        new Connection().execute();
        Connection.setPresenter(this);
    }

    public void logIn(String username, char[] password) {
        if (areInvalid(username, password)) return;

        view.disable();
        enteredUsername = username;
        enteredPassword = password;
        Connection.send(Command.Output.HASH_REQUEST, username);
    }

    public void register(String username, char[] password) {
        if (areInvalid(username, password)) return;

        view.disable();
        enteredUsername = username;
        String hashedPassword = BCrypt.hashpw(new String(password), BCrypt.gensalt());
        Arrays.fill(password, '0');
        Connection.send(Command.Output.REGISTRATION, username, hashedPassword);
    }

    public void closeApp() {
        Connection.send(Command.Output.EXIT);
        System.exit(0);
    }

    private boolean areInvalid(String username, char[] password) {
        if (username.isEmpty() || password.length == 0) {
            view.displayWarning("Please fill in all the fields");
            return true;
        }
        if (username.contains(Command.DELIMITER)) {
            view.displayWarning("A username cannot contain spaces");
            return true;
        }
        return false;
    }

    @Override
    public void processInput(String inputLine) {
        String[] input = inputLine.split(Command.DELIMITER);
        switch (Command.Input.get(input[0])) {
            case ALLOWED_REGISTRATION:
                openChatWindow();
                break;
            case PASSWORD_HASH:
                String hash = input[1];
                if (BCrypt.checkpw(new String(enteredPassword), hash)) {
                    openChatWindow();
                    Connection.send(Command.Output.LOGIN, enteredUsername);
                } else {
                    view.displayWarning("Incorrect username or password");
                }
                Arrays.fill(enteredPassword, '0');
                break;
            case DENIED_LOGIN:
                view.displayWarning("Incorrect username or password");
                break;
            case DENIED_REGISTRATION:
                view.displayWarning("This name is already taken");
                break;
        }
        view.enable();
    }

    @Override
    public void stop() {
        view.disable();
        view.displayWarning("Failed to connect to server");
    }

    private void openChatWindow() {
        view.close();
        new ChatView(enteredUsername);
    }
}