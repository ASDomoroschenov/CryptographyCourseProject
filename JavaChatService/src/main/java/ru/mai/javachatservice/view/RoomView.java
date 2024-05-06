package ru.mai.javachatservice.view;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.WildcardParameter;
import com.vaadin.flow.server.StreamResource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import ru.mai.javachatservice.cipher.Cipher;
import ru.mai.javachatservice.kafka.KafkaWriter;
import ru.mai.javachatservice.model.messages.CipherInfoMessage;
import ru.mai.javachatservice.model.messages.KeyMessage;
import ru.mai.javachatservice.model.messages.Message;
import ru.mai.javachatservice.model.messages.json_parser.CipherInfoMessageParser;
import ru.mai.javachatservice.model.messages.json_parser.MessageParser;
import ru.mai.javachatservice.server.ChatServer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Route("room")
public class RoomView extends VerticalLayout implements HasUrlParameter<String> {
    private final ChatServer server;
    private long clientId;
    private long roomId;
    private final KafkaWriter kafkaWriter;
    private String outputTopic;
    private volatile Cipher cipherEncrypt;
    private final ExecutorService service = Executors.newSingleThreadExecutor();
    private MessagesLayoutWrapper messagesLayoutWrapper;
    private long anotherClientId;
    private final Backend backend;

    @Override
    public void setParameter(BeforeEvent event, @WildcardParameter String parameter) {
        log.info("set parameter");

        String[] params = parameter.split("/");

        clientId = Long.parseLong(params[0]);
        roomId = Long.parseLong(params[1]);

        if (server.notExistClient(clientId)) {
            Notification.show("Пользователь не найден");
            setEnabled(false);
        } else {
            service.submit(backend::startKafka);
            server.addWindow("room/" + clientId + "/" + roomId, event.getUI());
        }
    }

    public RoomView(ChatServer server, KafkaWriter kafkaWriter) {
        this.server = server;
        this.kafkaWriter = kafkaWriter;
        this.outputTopic = null;
        this.cipherEncrypt = null;
        new Frontend().setPage();
        this.backend = new Backend();
    }

    @Override
    protected void onDetach(DetachEvent event) {
        server.disconnectFromRoom(clientId, roomId);

        if (outputTopic != null) {
            kafkaWriter.processing(new Message("disconnect", null, null, 0, null).toBytes(), outputTopic);
        }

        server.disconnectFromRoom(clientId, roomId);
        backend.close();

        service.shutdown();

        try {
            if (!service.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                service.shutdownNow();
            }
        } catch (InterruptedException e) {
            service.shutdownNow();
            Thread.currentThread().interrupt();
        }

        log.info("end service");
    }

    public class MessagesLayoutWrapper {
        private final VerticalLayout messagesLayout;
        private final KafkaWriter kafkaWriter;

        public enum Destination {
            OWN,
            ANOTHER
        }

        public MessagesLayoutWrapper(VerticalLayout messagesLayout, KafkaWriter kafkaWriter) {
            this.messagesLayout = messagesLayout;
            this.kafkaWriter = kafkaWriter;
        }

        public void showTextMessage(String textMessage, Destination destination) {
            Optional<UI> uiOptional = getUI();

            if (uiOptional.isPresent()) {
                UI ui = uiOptional.get();

                ui.access(() -> {
                    Div messageDiv = new Div();
                    messageDiv.setText(textMessage);

                    if (destination.equals(Destination.OWN)) {
                        messageDiv.getStyle()
                                .set("margin-left", "auto")
                                .set("background-color", "#cceeff");

                        setPossibilityToDelete(messagesLayout, messageDiv);
                    } else {
                        messageDiv.getStyle()
                                .set("margin-right", "auto")
                                .set("background-color", "#f2f2f2");
                    }

                    messageDiv.getStyle()
                            .set("border-radius", "5px")
                            .set("padding", "10px")
                            .set("border", "1px solid #ddd");

                    messagesLayout.add(messageDiv);
                    messagesLayout.getElement().executeJs("this.scrollTo(0, this.scrollHeight);");
                });
            }
        }

        public void showImageMessage(String nameFile, byte[] data, Destination destination) {
            Optional<UI> uiOptional = getUI();

            if (uiOptional.isPresent()) {
                UI ui = uiOptional.get();

                ui.access(() -> {
                    Div imageDiv = new Div();

                    StreamResource resource = new StreamResource(nameFile, () -> new ByteArrayInputStream(data));
                    Image image = new Image(resource, "Uploaded image");

                    imageDiv.add(image);

                    if (destination.equals(Destination.OWN)) {
                        imageDiv.getStyle()
                                .set("margin-left", "auto")
                                .set("background-color", "#cceeff");
                        setPossibilityToDelete(messagesLayout, imageDiv);
                    } else {
                        imageDiv.getStyle()
                                .set("margin-right", "auto")
                                .set("background-color", "#f2f2f2");
                    }

                    imageDiv.getStyle()
                            .set("overflow", "hidden")
                            .set("padding", "10px")
                            .set("border-radius", "5px")
                            .set("border", "1px solid #ddd")
                            .set("width", "60%")
                            .set("flex-shrink", "0");

                    image.getStyle()
                            .set("width", "100%")
                            .set("height", "100%");

                    messagesLayout.add(imageDiv);
                    messagesLayout.getElement().executeJs("this.scrollTo(0, this.scrollHeight);");
                });
            }
        }

        public void showFileMessage(String nameFile, byte[] data, Destination destination) {
            Optional<UI> uiOptional = getUI();

            if (uiOptional.isPresent()) {
                UI ui = uiOptional.get();

                ui.access(() -> {
                    Div fileDiv = new Div();
                    StreamResource resource = new StreamResource(nameFile, () -> new ByteArrayInputStream(data));

                    Anchor downloadLink = new Anchor(resource, "");
                    downloadLink.getElement().setAttribute("download", true);

                    Button downloadButton = new Button(nameFile, event -> downloadLink.getElement().callJsFunction("click"));

                    fileDiv.add(downloadButton, downloadLink);

                    if (destination.equals(Destination.OWN)) {
                        fileDiv.getStyle()
                                .set("margin-left", "auto")
                                .set("background-color", "#cceeff");

                        setPossibilityToDelete(messagesLayout, fileDiv);
                    } else {
                        fileDiv.getStyle()
                                .set("margin-right", "auto")
                                .set("background-color", "#f2f2f2");
                    }

                    fileDiv.getStyle()
                            .set("display", "inline-block")
                            .set("max-width", "80%")
                            .set("overflow", "hidden")
                            .set("padding", "10px")
                            .set("border-radius", "5px")
                            .set("border", "1px solid #ddd")
                            .set("flex-shrink", "0");

                    messagesLayout.add(fileDiv);
                    messagesLayout.getElement().executeJs("this.scrollTo(0, this.scrollHeight);");
                });
            }
        }

        private void setPossibilityToDelete(VerticalLayout messagesLayout, Div fileDiv) {
            messagesLayout.getElement().executeJs("this.scrollTo(0, this.scrollHeight);");

            fileDiv.addClickListener(event -> {
                int indexMessage = messagesLayout.indexOf(fileDiv);
                messagesLayout.remove(fileDiv);
                kafkaWriter.processing(new Message("delete_message", "text", null, indexMessage, null).toBytes(), outputTopic);
            });
        }

        private void clearMessages() {
            Optional<UI> uiOptional = getUI();

            if (uiOptional.isPresent()) {
                UI ui = uiOptional.get();
                ui.access(messagesLayout::removeAll);
            }
        }

        private void deleteMessage(int index) {
            Optional<UI> uiOptional = getUI();

            if (uiOptional.isPresent()) {
                UI ui = uiOptional.get();
                ui.access(() -> {
                    Component componentToRemove = messagesLayout.getComponentAt(index);
                    messagesLayout.remove(componentToRemove);
                });
            }
        }
    }

    public class Frontend {
        private static final String TYPE_MESSAGE = "message";
        private final TextField messageField;
        private final List<Pair<String, InputStream>> filesData = new ArrayList<>();

        public Frontend() {
            messageField = new TextField();
            messageField.setWidth("410px");
        }

        public void setPage() {
            setAlignItems(Alignment.CENTER);
            setSizeFull();
            setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

            VerticalLayout messagesLayout = getMessagesLayout();
            HorizontalLayout inputLayout = getInputLayout();

            add(messagesLayout, inputLayout);

            messagesLayoutWrapper = new MessagesLayoutWrapper(messagesLayout, kafkaWriter);
        }

        public void sendMessage(Upload upload) {
            if (cipherEncrypt == null) {
                Notification.show("Ошибка: не удалось отправить сообщение");
            } else {
                try {
                    for (Pair<String, InputStream> file : filesData) {
                        byte[] bytesFile = readBytesFromInputStream(file.getRight());
                        String format = getTypeFormat(file.getLeft());
                        Message message = new Message(TYPE_MESSAGE, format, file.getLeft(), 0, bytesFile);
                        byte[] messageBytes = message.toBytes();
                        kafkaWriter.processing(cipherEncrypt.encrypt(messageBytes), outputTopic);
                        server.saveMessage(clientId, anotherClientId, message);

                        if (format.equals("image")) {
                            messagesLayoutWrapper.showImageMessage(file.getLeft(), bytesFile, MessagesLayoutWrapper.Destination.OWN);
                        } else {
                            messagesLayoutWrapper.showFileMessage(file.getLeft(), bytesFile, MessagesLayoutWrapper.Destination.OWN);
                        }
                    }

                    upload.clearFileList();
                    filesData.clear();

                    String textMessage = messageField.getValue();

                    if (!textMessage.isEmpty()) {
                        Message message = new Message(TYPE_MESSAGE, "text", "text", 0, textMessage.getBytes());
                        byte[] messageBytes = message.toBytes();
                        kafkaWriter.processing(cipherEncrypt.encrypt(messageBytes), outputTopic);
                        server.saveMessage(clientId, anotherClientId, message);
                        messagesLayoutWrapper.showTextMessage(textMessage, MessagesLayoutWrapper.Destination.OWN);
                    }

                    messageField.clear();
                } catch (IOException | RuntimeException | ExecutionException ex) {
                    log.error(ex.getMessage());
                    log.error(Arrays.deepToString(ex.getStackTrace()));
                } catch (InterruptedException ex) {
                    log.error(ex.getMessage());
                    log.error(Arrays.deepToString(ex.getStackTrace()));
                    Thread.currentThread().interrupt();
                }
            }
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
            sendButtonText.addClickListener(e -> sendMessage(upload));
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
            Upload uploadButton = new Upload(multiFileMemoryBuffer);
            Button buttonLoadFile = new Button("+");

            buttonLoadFile.setWidth("60px");

            uploadButton.setUploadButton(buttonLoadFile);
            uploadButton.setWidth("620px");
            uploadButton.getStyle()
                    .set("padding", "0")
                    .set("margin", "0")
                    .set("border", "none");
            uploadButton.setDropLabel(new Span(""));
            uploadButton.setDropLabelIcon(new Span(""));

            uploadButton.addSucceededListener(event -> {
                String fileName = event.getFileName();
                filesData.add(Pair.of(fileName, multiFileMemoryBuffer.getInputStream(fileName)));
            });

            return uploadButton;
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

        String getTypeFormat(String fileName) {
            int lastDotIndex = fileName.lastIndexOf('.');
            String extension = fileName.substring(lastDotIndex + 1);

            if (extension.equals("jpg") || extension.equals("png") || extension.equals("jpeg")) {
                return "image";
            }

            return "other";
        }
    }

    public class Backend {
        private static final String bootstrapServer = "localhost:9093";
        private static final String autoOffsetReset = "earliest";
        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
        private static final Random RANDOM = new Random();
        private volatile Cipher cipherDecrypt;
        private volatile boolean isRunning = true;
        private CipherInfoMessage cipherInfoAnotherClient;
        private byte[] privateKey;
        private byte[] publicKeyAnother;
        private byte[] p;

        public void startKafka() {
            CipherInfoMessage cipherInfoThisClient = server.getCipherInfoMessageClient(clientId, roomId);

            KafkaConsumer<byte[], byte[]> kafkaConsumer = new KafkaConsumer<>(
                    Map.of(
                            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer,
                            ConsumerConfig.GROUP_ID_CONFIG, "group_" + clientId + "_" + roomId,
                            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset
                    ),
                    new ByteArrayDeserializer(),
                    new ByteArrayDeserializer()
            );
            kafkaConsumer.subscribe(Collections.singletonList("input_" + clientId + "_" + roomId));

            try {
                while (isRunning) {
                    ConsumerRecords<byte[], byte[]> consumerRecords = kafkaConsumer.poll(Duration.ofMillis(1000));

                    for (ConsumerRecord<byte[], byte[]> consumerRecord : consumerRecords) {
                        String jsonMessage = new String(consumerRecord.value());

                        if (jsonMessage.contains("cipher_info")) {
                            cipherInfoAnotherClient = OBJECT_MAPPER.readValue(jsonMessage, CipherInfoMessage.class);

                            outputTopic = "input_" + cipherInfoAnotherClient.getAnotherClientId() + "_" + roomId;
                            privateKey = generatePrivateKey();
                            p = cipherInfoAnotherClient.getP();
                            anotherClientId = cipherInfoAnotherClient.getAnotherClientId();
                            byte[] publicKey = generatePublicKey(privateKey, p, cipherInfoAnotherClient.getG());

                            log.info("Client {} get cipher info", clientId);
                            log.info(cipherInfoAnotherClient.toString());

                            kafkaWriter.processing(new KeyMessage("key_info", publicKey).toBytes(), outputTopic);

                            if (publicKeyAnother != null) {
                                cipherInfoAnotherClient.setPublicKey(publicKeyAnother);
                                cipherDecrypt = CipherInfoMessageParser.getCipher(cipherInfoAnotherClient, new BigInteger(privateKey), new BigInteger(p));

                                cipherInfoThisClient.setPublicKey(publicKeyAnother);
                                cipherEncrypt = CipherInfoMessageParser.getCipher(cipherInfoThisClient, new BigInteger(privateKey), new BigInteger(p));
                            }
                        } else if (jsonMessage.contains("key_info")) {
                            log.info("Client {} get key info", clientId);

                            KeyMessage keyMessage = OBJECT_MAPPER.readValue(jsonMessage, KeyMessage.class);

                            if (cipherInfoAnotherClient != null) {
                                cipherInfoAnotherClient.setPublicKey(keyMessage.getPublicKey());
                                cipherDecrypt = CipherInfoMessageParser.getCipher(cipherInfoAnotherClient, new BigInteger(privateKey), new BigInteger(p));

                                cipherInfoThisClient.setPublicKey(keyMessage.getPublicKey());
                                cipherEncrypt = CipherInfoMessageParser.getCipher(cipherInfoThisClient, new BigInteger(privateKey), new BigInteger(p));
                            } else {
                                publicKeyAnother = keyMessage.getPublicKey();
                            }
                        } else if (jsonMessage.contains("delete_message")) {
                            Message deleteMessage = OBJECT_MAPPER.readValue(jsonMessage, Message.class);
                            messagesLayoutWrapper.deleteMessage(deleteMessage.getIndexMessage());
                        } else if (jsonMessage.contains("disconnect")) {
                            cipherDecrypt = null;
                            cipherEncrypt = null;
                            messagesLayoutWrapper.clearMessages();
                        } else {
                            Message message = MessageParser.parseMessage(new String(cipherDecrypt.decrypt(consumerRecord.value())));

                            if (message != null && message.getBytes() != null) {
                                log.info("Client {} get message", clientId);

                                server.saveMessage(anotherClientId, clientId, message);

                                if (message.getTypeFormat().equals("text")) {
                                    messagesLayoutWrapper.showTextMessage(new String(message.getBytes()), MessagesLayoutWrapper.Destination.ANOTHER);
                                } else if (message.getTypeFormat().equals("image")) {
                                    messagesLayoutWrapper.showImageMessage(message.getFileName(), message.getBytes(), MessagesLayoutWrapper.Destination.ANOTHER);
                                } else {
                                    messagesLayoutWrapper.showFileMessage(message.getFileName(), message.getBytes(), MessagesLayoutWrapper.Destination.ANOTHER);
                                }
                            }
                        }
                    }
                }
            } catch (InterruptedException ex) {
                log.error(ex.getMessage());
                log.error(Arrays.deepToString(ex.getStackTrace()));
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                log.error(ex.getMessage());
                log.error(Arrays.deepToString(ex.getStackTrace()));
            }

            kafkaConsumer.close();

            log.info("End kafka reader client {}", clientId);
        }

        private byte[] generatePrivateKey() {
            return new BigInteger(100, RANDOM).toByteArray();
        }

        private byte[] generatePublicKey(byte[] privateKey, byte[] p, byte[] g) {
            BigInteger pNumber = new BigInteger(p);
            BigInteger gNumber = new BigInteger(g);
            BigInteger key = new BigInteger(privateKey);
            return gNumber.modPow(key, pNumber).toByteArray();
        }

        public void close() {
            isRunning = false;
        }
    }
}