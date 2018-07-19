package client.Login;

import client.*;
import client.Chat.ChatView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public final class LoginPresenter implements client.Presenter {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginPresenter.class);
    private final View view;
    private String enteredUsername;

    LoginPresenter(View view) {
        this.view = view;
        new Connection().execute();
        Connection.setPresenter(this);
        LOGGER.debug("Login presenter is active");
    }

    public void logIn(String username, char[] password) {
        if (areInvalid(username, password)) return;

        view.disable();
        enteredUsername = username;

        Connection.send(Command.Output.LOGIN, username, new String(password));
        Arrays.fill(password, '0');
        LOGGER.debug("Trying to log in with {} as username", username);
    }

    public void register(String username, char[] password) {
        if (areInvalid(username, password)) return;

        view.disable();
        enteredUsername = username;

        Connection.send(Command.Output.REGISTRATION, username, new String(password));
        Arrays.fill(password, '0');
        LOGGER.debug("Trying to register with {} as username", username);
    }

    public void closeApp() {
        Connection.send(Command.Output.EXIT);
        LOGGER.info("Disconnection and exit");
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
        Command.Input command = Command.Input.get(inputLine);
        LOGGER.trace("From server: {}", command.name());
        switch (command) {
            case ALLOWED_LOGIN:
                openChatWindow();
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
        LOGGER.info("Login window has been closed");
        new ChatView(enteredUsername);
        LOGGER.info("Chat window has been opened for user {}", enteredUsername);
    }
}