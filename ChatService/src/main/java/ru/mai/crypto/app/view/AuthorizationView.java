package ru.mai.crypto.app.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;
import ru.mai.crypto.app.Server;

@Slf4j
@Route("")
public class AuthorizationView extends VerticalLayout {
    public AuthorizationView(Server server) {
        TextField usernameField = new TextField("Имя пользователя");

        ComboBox<String> encryptionAlgorithmComboBox = new ComboBox<>("Выберите алгоритм шифрования");
        encryptionAlgorithmComboBox.setItems("LOKI97", "RC5");
        encryptionAlgorithmComboBox.setValue("RC5");

        Button loginButton = new Button("Авторизоваться", event -> {
            String username = usernameField.getValue();
            String nameAlgorithm = encryptionAlgorithmComboBox.getValue();

            if (!username.isEmpty()) {
                if (!username.matches("^[a-zA-Z0-9]+$")) {
                    Notification.show("Ошибка авторизации: имя должно состоять из латинских букв");
                } else {
                    if (server.createClient(username, nameAlgorithm)) {
                        Notification.show("Вы успешно авторизованы");
                        UI.getCurrent().navigate(username);
                    } else {
                        Notification.show("Ошибка авторизации: пользователь с таким именем уже существует");
                    }
                }
            } else {
                Notification.show("Ошибка авторизации: имя пользователя не может быть пустым");
            }
        });

        loginButton.setWidth("195px");

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        add(usernameField, encryptionAlgorithmComboBox, loginButton);
    }
}
