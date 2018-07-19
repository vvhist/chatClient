package client.Chat;

import client.*;
import client.Login.LoginPresenter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public final class ChatPresenter implements client.Presenter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatPresenter.class);
    private final View view;
    private final String username;

    ChatPresenter(View view, String username) {
        this.view = view;
        this.username = username;
        Connection.setPresenter(this);
        LOGGER.debug("Chat presenter is active");
    }

    public void searchNewContact(String contact) {
        if (contact.isEmpty()) return;
        LOGGER.debug("Trying to search for a new contact {}", contact);

        if (view.hasTab(contact)) {
            view.displayWarning("Conversation with " + contact + " already exists.");
        } else {
            view.displayWarning(" ");
            Connection.send(Command.Output.NEW_CONTACT, contact);
        }
    }

    public void sendMessage(String contact, String text) {
        if (text.isEmpty()) return;
        Connection.send(Command.Output.NEW_MESSAGE, contact, text);
    }

    public void closeApp() {
        Connection.send(Command.Output.EXIT);
        LOGGER.info("Disconnection and exit");
        System.exit(0);
    }

    @Override
    public void processInput(String inputLine) {
        String[] input = inputLine.split(Command.DELIMITER, 2);
        Command.Input command = Command.Input.get(input[0]);
        LOGGER.trace("From server: {}", inputLine.replaceFirst(input[0], command.name()));
        switch (command) {
            case MESSAGE:
                processMessage(input[1]);
                break;
            case FOUND_USER:
                String contact = input[1];
                view.addTab(contact);
                LOGGER.info("Adding new contact {}", contact);
                break;
            case NOT_FOUND_USER:
                contact = input[1];
                if (contact.length() > 30) {
                    contact = contact.substring(0, 30) + "...";
                }
                view.displayWarning("User " + contact + " was not found");
                break;
        }
    }

    @Override
    public void stop() {
        view.disable();
        view.displayWarning("Failed to connect to server");
    }

    private void processMessage(String message) {
        String[] messageData = message.split(Command.DELIMITER, 4);
        String sender    = messageData[0];
        String recipient = messageData[1];
        String timestamp = messageData[2];
        String text      = messageData[3];

        String contact = username.equals(sender) ? recipient : sender;
        if (!view.hasTab(contact)) {
            view.addTab(contact);
        }
        String time = LocalDateTime.parse(timestamp)
                .toLocalTime()
                .truncatedTo(ChronoUnit.SECONDS)
                .format(DateTimeFormatter.ISO_LOCAL_TIME);
        String formattedMessage = time + " " + sender + ": " + text;
        view.displayMessage(contact, formattedMessage);
    }
}