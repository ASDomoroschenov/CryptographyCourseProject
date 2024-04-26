package ru.mai.crypto.room.view.impl;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.WildcardParameter;
import lombok.extern.slf4j.Slf4j;
import ru.mai.crypto.app.ServerRoom;
import ru.mai.crypto.room.room_client.RoomClient;
import ru.mai.crypto.room.view.RoomClientView;

@Slf4j
@Route("room")
public class RoomClientViewImpl extends VerticalLayout implements HasUrlParameter<String>, RoomClientView {
    private final ServerRoom server;
    private RoomClient roomClient;
    private final TextField messageField;
    private final VerticalLayout messagesLayout;

    @Override
    public void setParameter(BeforeEvent event, @WildcardParameter String parameter) {
        String[] parts = parameter.split("/");
        String name = parts[0];
        int roomId = Integer.parseInt(parts[1]);
        this.roomClient = server.connect(name, roomId, this);
        this.roomClient.setUi(event.getUI());
        this.roomClient.setMessageLayout(messagesLayout);
    }

    public RoomClientViewImpl(ServerRoom server) {
        this.server = server;

        setAlignItems(Alignment.CENTER);

        messagesLayout = new VerticalLayout();
        messagesLayout.getStyle().set("border", "1px dashed #4A90E2");
        messagesLayout.getStyle().set("border-radius", "5px");
        messagesLayout.getStyle().set("padding", "10px");
        messagesLayout.setWidth("500px");
        messagesLayout.setHeight("300px");
        messagesLayout.getStyle().set("overflow-y", "auto");

        messageField = new TextField();
        messageField.setWidth("400px");

        Button sendButton = new Button("Отправить");
        sendButton.addClickListener(e -> sendMessage(messagesLayout, messageField, roomClient));

        HorizontalLayout inputLayout = new HorizontalLayout(messageField, sendButton);
        inputLayout.setWidth("500px");
        inputLayout.setAlignItems(Alignment.BASELINE);
        inputLayout.setSpacing(true);

        add(messagesLayout, inputLayout);
    }
}