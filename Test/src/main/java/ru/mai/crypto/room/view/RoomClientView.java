package ru.mai.crypto.room.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import ru.mai.crypto.room.room_client.RoomClient;
import ru.mai.crypto.room.model.Message;

public interface RoomClientView {
    default void sendMessage(VerticalLayout messagesLayout, TextField messageField, RoomClient user) {
        String messageText = messageField.getValue();
        Div messageDiv = new Div();

        if (!messageText.isEmpty()) {
            user.sendMessage(new Message("message", "text", messageText.getBytes()));

            messageDiv.setText(messageText);
            messageDiv.getStyle().set("margin-left", "auto");
            messageDiv.getStyle().set("border-radius", "5px");
            messageDiv.getStyle().set("padding", "10px");
            messageDiv.getStyle().set("background-color", "#cceeff");
            messageDiv.getStyle().set("border", "1px solid #4A90E2");

            messagesLayout.add(messageDiv);

            messageField.clear();
        }
    }

    default void showMessage(UI ui, Message message, VerticalLayout messagesLayout) {
        ui.access(() -> {
            Div messageDiv = new Div();

            messageDiv.setText(new String(message.getBytes()));
            messageDiv.getStyle().set("margin-right", "auto");
            messageDiv.getStyle().set("border-radius", "5px");
            messageDiv.getStyle().set("padding", "10px");
            messageDiv.getStyle().set("background-color", "#f2f2f2");
            messageDiv.getStyle().set("border", "1px solid #ddd");

            messagesLayout.add(messageDiv);
        });
    }
}
