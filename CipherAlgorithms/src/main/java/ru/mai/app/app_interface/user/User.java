package ru.mai.app.app_interface.user;

import ru.mai.app.model.Message;

public interface User {
    public void sendMessage(Message message);
    public void processing();
    public long getUserId();
}
