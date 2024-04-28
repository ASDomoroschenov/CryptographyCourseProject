package ru.mai.crypto.room.view.impl;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.WildcardParameter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import ru.mai.crypto.app.Server;
import ru.mai.crypto.app.ServerRoom;
import ru.mai.crypto.room.room_client.RoomClient;
import ru.mai.crypto.room.view.RoomClientView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Route("room")
public class RoomClientViewImpl extends VerticalLayout implements HasUrlParameter<String>, RoomClientView {
    private final ServerRoom serverRoom;
    private final Server server;
    private RoomClient roomClient;
    private final TextField messageField;
    private final VerticalLayout messagesLayout;
    private final List<Pair<String, InputStream>> filesData = new ArrayList<>();
    private final List<Component> messages;

    @Override
    public void setParameter(BeforeEvent event, @WildcardParameter String parameter) {
        String[] parts = parameter.split("/");
        String name = parts[0];
        int roomId = Integer.parseInt(parts[1]);
        this.roomClient = serverRoom.connect(name, roomId, this);
        this.roomClient.setUi(event.getUI());
        this.roomClient.setMessageLayout(messagesLayout);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        serverRoom.removeWindow("room/" + roomClient.getName() + "/" + roomClient.getRoomId());
        serverRoom.leaveRoom(roomClient);
        server.leaveRoom(roomClient.getName(), roomClient.getRoomId());
        super.onDetach(detachEvent);
    }

    public RoomClientViewImpl(ServerRoom serverRoom, Server server) {
        this.serverRoom = serverRoom;
        this.server = server;
        this.messages = new ArrayList<>();

        setAlignItems(Alignment.CENTER);

        messagesLayout = getMessagesLayout();

        messageField = new TextField();
        messageField.setWidth("410px");

        HorizontalLayout inputLayout = getInputLayout();

        setSizeFull();
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        add(messagesLayout, inputLayout);
    }

    private HorizontalLayout getInputLayout() {
        HorizontalLayout horizontalLayout = getHorizontalLayout();
        horizontalLayout.setWidth("700px");
        horizontalLayout.setAlignItems(Alignment.BASELINE);
        horizontalLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        horizontalLayout.setSpacing(true);
        horizontalLayout.getStyle().set("padding-left", "90px");

        VerticalLayout verticalLayout = new VerticalLayout(horizontalLayout);
        verticalLayout.setWidth("700px");
        verticalLayout.setAlignItems(Alignment.CENTER);
        verticalLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        verticalLayout.getStyle().set("position", "relative");

        return horizontalLayout;
    }

    private HorizontalLayout getHorizontalLayout() {
        Upload upload = getUploadButton();
        Button sendButtonText = new Button("Отправить");

        sendButtonText.addClickListener(e -> {
            for (Pair<String, InputStream> file : filesData) {
                try {
                    byte[] bytesFile = readBytesFromInputStream(file.getRight());

                    if (!sendFile(messagesLayout, file.getLeft(), bytesFile, roomClient)) {
                        Notification.show("Ошибка: не удалось отправить файл");
                        messageField.clear();
                    }
                } catch (IOException ex) {
                    log.error(ex.getMessage());
                    log.error(Arrays.deepToString(ex.getStackTrace()));
                }
            }

            upload.clearFileList();
            filesData.clear();

            if (!sendMessage(messagesLayout, messageField, roomClient)) {
                Notification.show("Ошибка: не удалось отправить сообщение");
                messageField.clear();
            }
        });

        upload.getElement().getStyle().set("position", "absolute").set("top", "610px").set("left", "457px");
        add(upload);

        return new HorizontalLayout(messageField, sendButtonText);
    }

    private byte[] readBytesFromInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int bytesRead;
        byte[] data = new byte[1024];

        while ((bytesRead = inputStream.read(data, 0, data.length)) > 0) {
            buffer.write(data, 0, bytesRead);
        }

        buffer.flush();

        return buffer.toByteArray();
    }

    private Upload getUploadButton() {
        MultiFileMemoryBuffer multiFileMemoryBuffer = new MultiFileMemoryBuffer();
        Upload upload = new Upload(multiFileMemoryBuffer);
        Button buttonLoadFile = new Button("+");

        buttonLoadFile.setWidth("60px");

        upload.setUploadButton(buttonLoadFile);
        upload.setWidth("620px");
        upload.getStyle()
                .set("padding", "0")
                .set("margin", "0")
                .set("border", "none");
        upload.setDropLabel(new Span(""));
        upload.setDropLabelIcon(new Span(""));

        upload.addSucceededListener(event -> {
            String fileName = event.getFileName();
            System.out.println(multiFileMemoryBuffer.getInputStream(fileName) == null);
            filesData.add(Pair.of(fileName, multiFileMemoryBuffer.getInputStream(fileName)));
        });

        return upload;
    }

    private VerticalLayout getMessagesLayout() {
        VerticalLayout layout = new VerticalLayout();

        layout.getStyle()
                .set("max-width", "620px")
                .set("max-height", "500px")
                .set("border", "1px dashed #4A90E2")
                .set("border-radius", "5px")
                .set("padding", "10px")
                .set("overflow-y", "auto");

        layout.setWidth("620px");
        layout.setHeight("500px");

        return layout;
    }
}