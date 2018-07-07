package client.Chat;

import client.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public final class ChatPresenter implements client.Presenter {

    private final View view;
    private final String username;

    ChatPresenter(View view, String username) {
        this.view = view;
        this.username = username;
        Connection.setPresenter(this);
    }

    public void searchNewContact(String contact) {
        if (contact.isEmpty()) return;

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
        System.exit(0);
    }

    @Override
    public void processInput(String inputLine) {
        String[] input = inputLine.split(Command.DELIMITER, 5);
        switch (Command.Input.get(input[0])) {
            case MESSAGE:
                processMessage(input[1], input[2], input[3], input[4]);
                break;
            case FOUND_USER:
                String contact = input[1];
                view.addTab(contact);
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

    private void processMessage(String sender, String recipient, String timestamp, String text) {
        String contact = username.equals(sender) ? recipient : sender;
        if (!view.hasTab(contact)) {
            view.addTab(contact);
        }
        String time = LocalDateTime.parse(timestamp)
                .toLocalTime()
                .truncatedTo(ChronoUnit.SECONDS)
                .format(DateTimeFormatter.ISO_LOCAL_TIME);
        String message = time + " " + sender + ": " + text;
        view.displayMessage(contact, message);
    }
}