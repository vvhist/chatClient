package client.Chat;

public interface View {

    boolean hasTab(String title);

    void addTab(String title);

    void displayMessage(String tabTitle, String message);

    void displayWarning(String warning);

    void disable();
}