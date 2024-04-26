package ru.mai.crypto.app.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
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

        Button loginButton = new Button("Авторизоваться", event -> {
            String username = usernameField.getValue();

            if (!username.isEmpty()) {
                if (server.createClient(username)) {
                    Notification.show("Вы успешно авторизованы");
                    UI.getCurrent().navigate(username);
                } else {
                    Notification.show("Ошибка авторизации: пользователь с таким именем уже существует");
                }
            } else {
                Notification.show("Ошибка авторизации: имя пользователя не может быть пустым");
            }
        });

        add(usernameField, loginButton);
    }
}
