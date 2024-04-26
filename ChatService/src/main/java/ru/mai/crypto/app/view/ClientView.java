package ru.mai.crypto.app.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;
import ru.mai.crypto.app.Server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Route("")
public class ClientView extends VerticalLayout implements HasUrlParameter<String> {
    private String nameClient;
    private final TextField roomIdField;
    private final Map<String, List<UI>> openWindows = new HashMap<>();
    private final Server server;

    @Override
    public void setParameter(BeforeEvent event, String parameter) {
        this.nameClient = parameter;
    }

    public ClientView(Server server) {
        this.server = server;

        log.info("ClientView start");
        HorizontalLayout startChatLayout = new HorizontalLayout();
        roomIdField = new TextField();
        roomIdField.setPlaceholder("Введите ID комнаты");
        roomIdField.setWidth("500px");
        Button startChatButton = new Button("Начать чат", event -> startChat());

        startChatLayout.getStyle().set("padding", "10px");

        startChatLayout.add(roomIdField, startChatButton);
        add(startChatLayout);
    }

    private void startChat() {
        String roomId = roomIdField.getValue();

        if (roomId.isEmpty()) {
            Notification.show("ID комнаты не может быть пустым");
            return;
        }

        String url = "room/" + nameClient + "/" + roomId;
        UI ui = UI.getCurrent();

        ui.getPage().executeJs("window.open($0, '_blank')", url);
        openWindows.computeIfAbsent(url, k -> new ArrayList<>()).add(ui);

        addChatInfoBlock(roomId);
    }

    private void addChatInfoBlock(String roomId) {
        HorizontalLayout chatInfoLayout = new HorizontalLayout();
        chatInfoLayout.setWidth("650px");
        chatInfoLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);

        chatInfoLayout.getStyle().set("border", "2px dashed #00BFFF");
        chatInfoLayout.getStyle().set("padding", "10px");
        chatInfoLayout.getStyle().set("border-radius", "5px");

        String url = "room/" + nameClient + "/" + roomId;
        Button chatInfoButton = new Button("Комната: " + roomId,
                e -> UI.getCurrent().getPage().executeJs("window.open($0, '_blank')", url));
        chatInfoButton.setWidth("500px");

        Button leaveChatButton = getLeaveChatButton(roomId, chatInfoLayout);
        chatInfoLayout.add(chatInfoButton, leaveChatButton);
        chatInfoLayout.expand(chatInfoButton);
        add(chatInfoLayout);
    }

    private Button getLeaveChatButton(String roomId, HorizontalLayout chatInfoLayout) {
        Button leaveChatButton = new Button("Покинуть", e -> {
            int id = Integer.parseInt(roomId);
            server.leaveRoom(nameClient, id);
            removeChatInfoBlock(chatInfoLayout);
        });

        leaveChatButton.setWidth("110px");

        return leaveChatButton;
    }

    private void removeChatInfoBlock(HorizontalLayout chatInfoLayout) {
        remove(chatInfoLayout);
    }
}
