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
import ru.mai.crypto.app.ServerRoom;

@Slf4j
@Route("")
public class ClientView extends VerticalLayout implements HasUrlParameter<String> {
    private String nameClient;
    private final TextField roomIdField;
    private final ServerRoom serverRoom;


    @Override
    public void setParameter(BeforeEvent event, String parameter) {
        this.nameClient = parameter;
    }

    public ClientView(ServerRoom serverRoom) {
        this.serverRoom = serverRoom;

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

        if (serverRoom.canConnect(nameClient, Integer.parseInt(roomId))) {
            if (serverRoom.isNotOpenWindow(url)) {
                ui.getPage().executeJs("window.open($0, '_blank')", url);
                serverRoom.addWindow(url);
                addChatInfoBlock(roomId);
            } else {
                Notification.show("Ошибка: чат уже открыт");
            }
        } else {
            Notification.show("Ошибка подключения: комната уже занята");
        }
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
                e -> {
                    if (serverRoom.isNotOpenWindow(url) && serverRoom.canConnect(nameClient, Integer.parseInt(roomId))) {
                        UI.getCurrent().getPage().executeJs("window.open($0, '_blank')", url);
                        serverRoom.addWindow(url);
                    } else {
                        if (!serverRoom.isNotOpenWindow(url)) {
                            Notification.show("Ошибка подключения: вы уже находитесь в комнате");
                        } else {
                            Notification.show("Ошибка подключения: комната уже занята");
                        }
                    }
                });
        chatInfoButton.setWidth("500px");

        Button leaveChatButton = getLeaveChatButton(roomId, chatInfoLayout);
        chatInfoLayout.add(chatInfoButton, leaveChatButton);
        chatInfoLayout.expand(chatInfoButton);
        add(chatInfoLayout);
    }

    private Button getLeaveChatButton(String roomId, HorizontalLayout chatInfoLayout) {
        Button leaveChatButton = new Button("Покинуть", e -> {
            serverRoom.disconnect(nameClient, Integer.parseInt(roomId));
            removeChatInfoBlock(chatInfoLayout);
        });

        leaveChatButton.setWidth("110px");

        return leaveChatButton;
    }

    private void removeChatInfoBlock(HorizontalLayout chatInfoLayout) {
        remove(chatInfoLayout);
    }
}
