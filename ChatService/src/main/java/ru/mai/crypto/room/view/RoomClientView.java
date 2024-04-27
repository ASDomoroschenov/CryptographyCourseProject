package ru.mai.crypto.room.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.StreamResource;
import ru.mai.crypto.room.model.Message;
import ru.mai.crypto.room.room_client.RoomClient;

import java.io.ByteArrayInputStream;

public interface RoomClientView {
    default boolean sendFile(VerticalLayout messagesLayout, String fileName, byte[] fileData, RoomClient user) {
        String format = getTypeFormat(fileName);

        if (fileData != null && fileData.length > 0) {
            boolean isSend = user.sendMessage(new Message("message", format, fileName, fileData));

            if (isSend) {
                if (format.equals("image")) {
                    Div imageDiv = new Div();

                    StreamResource resource = new StreamResource(fileName, () -> new ByteArrayInputStream(fileData));
                    Image image = new Image(resource, "Uploaded image");

                    imageDiv.add(image);

                    imageDiv.getStyle()
                            .set("margin-left", "auto")
                            .set("overflow", "hidden")
                            .set("padding", "10px")
                            .set("border-radius", "5px")
                            .set("background-color", "#cceeff")
                            .set("border", "1px solid #ddd")
                            .set("width", "60%")
                            .set("flex-shrink", "0");

                    image.getStyle()
                            .set("width", "100%")
                            .set("height", "100%");

                    messagesLayout.add(imageDiv);
                    messagesLayout.getElement().executeJs("this.scrollTo(0, this.scrollHeight);");
                } else {
                    Div fileDiv = new Div();
                    StreamResource resource = new StreamResource(fileName, () -> new ByteArrayInputStream(fileData));

                    Anchor downloadLink = new Anchor(resource, "");
                    downloadLink.getElement().setAttribute("download", true);

                    Button downloadButton = new Button(fileName, event -> downloadLink.getElement().callJsFunction("click"));

                    fileDiv.add(downloadButton, downloadLink);

                    fileDiv.getStyle()
                            .set("margin-left", "auto")
                            .set("display", "inline-block")
                            .set("max-width", "80%")
                            .set("overflow", "hidden")
                            .set("padding", "10px")
                            .set("border-radius", "5px")
                            .set("background-color", "#cceeff")
                            .set("border", "1px solid #ddd")
                            .set("flex-shrink", "0");

                    messagesLayout.add(fileDiv);
                    messagesLayout.getElement().executeJs("this.scrollTo(0, this.scrollHeight);");
                }

                return true;
            }

            return false;
        }

        return false;
    }

    default String getTypeFormat(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        String extension = fileName.substring(lastDotIndex + 1);

        if (extension.equals("jpg") || extension.equals("png") || extension.equals("jpeg")) {
            return "image";
        }

        return "other";
    }

    default boolean sendMessage(VerticalLayout messagesLayout, TextField messageField, RoomClient user) {
        String messageText = messageField.getValue();
        Div messageDiv = new Div();

        if (!messageText.isEmpty()) {
            boolean isSend = user.sendMessage(new Message("message", "text", "text", messageText.getBytes()));

            if (isSend) {
                messageDiv.setText(messageText);
                messageDiv.getStyle().set("margin-left", "auto");
                messageDiv.getStyle().set("border-radius", "5px");
                messageDiv.getStyle().set("padding", "10px");
                messageDiv.getStyle().set("background-color", "#cceeff");
                messageDiv.getStyle().set("border", "1px solid #4A90E2");

                messagesLayout.add(messageDiv);
                messagesLayout.getElement().executeJs("this.scrollTo(0, this.scrollHeight);");

                messageField.clear();

                return true;
            }

            return false;
        }

        return true;
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

            messagesLayout.getElement().executeJs("this.scrollTo(0, this.scrollHeight);");

            messagesLayout.add(messageDiv);
        });
    }

    default void showImage(UI ui, Message message, VerticalLayout messagesLayout) {
        ui.access(() -> {
            byte[] bytesImage = message.getBytes();

            if (bytesImage != null && bytesImage.length > 0) {
                StreamResource resource = new StreamResource("image.png", () -> new ByteArrayInputStream(bytesImage));
                Image image = new Image(resource, "Uploaded image");

                Div imageDiv = new Div();

                imageDiv.add(image);

                imageDiv.getStyle()
                        .set("margin-right", "auto")
                        .set("overflow", "hidden")
                        .set("padding", "10px")
                        .set("border-radius", "5px")
                        .set("background-color", "#f2f2f2")
                        .set("border", "1px solid #ddd")
                        .set("width", "60%")
                        .set("flex-shrink", "0");

                image.getStyle()
                        .set("width", "100%")
                        .set("height", "100%");

                messagesLayout.add(imageDiv);

                messagesLayout.getElement().executeJs("this.scrollTo(0, this.scrollHeight);");
            }
        });
    }

    default void showFile(UI ui, Message message, VerticalLayout messagesLayout) {
        ui.access(() -> {
            byte[] fileData = message.getBytes();
            String fileName = message.getFileName();

            if (fileData != null && fileData.length > 0) {
                Div fileDiv = new Div();
                StreamResource resource = new StreamResource(fileName, () -> new ByteArrayInputStream(fileData));

                Anchor downloadLink = new Anchor(resource, "");
                downloadLink.getElement().setAttribute("download", true);

                Button downloadButton = new Button(message.getFileName(), event -> downloadLink.getElement().callJsFunction("click"));

                downloadLink.add(downloadButton);

                fileDiv.add(downloadLink);

                fileDiv.getStyle()
                        .set("display", "inline-block")
                        .set("max-width", "80%")
                        .set("overflow", "hidden")
                        .set("padding", "10px")
                        .set("border-radius", "5px")
                        .set("background-color", "#f2f2f2")
                        .set("border", "1px solid #ddd")
                        .set("flex-shrink", "0");

                messagesLayout.add(fileDiv);
                messagesLayout.getElement().executeJs("this.scrollTo(0, this.scrollHeight);");
            }
        });
    }
}
