package client.Chat;

import client.*;

import javax.swing.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

public final class ChatPresenter implements Presenter {

    private final ChatView view;
    private final String username;
    private final Map<String, JTextArea> dialogs = new HashMap<>();

    ChatPresenter(ChatView view, String username) {
        this.view = view;
        this.username = username;
        Connection.setPresenter(this);
    }

    public void searchNewContact(String contact) {
        if (contact.isEmpty()) return;

        if (dialogs.containsKey(contact)) {
            view.displayWarning("Conversation with " + contact + " already exists.");
        } else {
            Connection.send(Command.Output.NEW_CONTACT, contact);
        }
    }

    public void sendMessage(String contact, String text) {
        if (text.isEmpty()) return;

        Connection.send(Command.Output.NEW_MESSAGE, contact, text);
    }

    @Override
    public void processInput(String inputLine) {
        String[] input = inputLine.split(Command.DELIMITER, 5);
        switch (Command.Input.get(input[0])) {
            case MESSAGE:
                String sender    = input[1];
                String recipient = input[2];
                String timestamp = input[3];
                String text      = input[4];

                String contact = username.equals(sender) ? recipient : sender;
                if (!dialogs.containsKey(contact)) {
                    dialogs.put(contact, view.getNewTab(contact));
                }
                String time = LocalDateTime.parse(timestamp)
                        .toLocalTime()
                        .truncatedTo(ChronoUnit.SECONDS)
                        .format(DateTimeFormatter.ISO_LOCAL_TIME);
                dialogs.get(contact).append(time + " " + sender + ": " + text + "\n");
                break;
            case FOUND_USER:
                contact = input[1];
                dialogs.put(contact, view.getNewTab(contact));
                view.displayWarning(" ");
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
        String warning = "Failed to connect to server";
        view.enableComponents(view.$$$getRootComponent$$$(), false);
        view.displayWarning(warning);
    }

    @Override
    public void closeApp() {
        Connection.send(Command.Output.EXIT);
        System.exit(0);
    }
}