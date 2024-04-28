package ru.mai.crypto.app.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class ClientInfo {
    private String name;
    private ClientCipherInfo clientCipherInfo;
    private Map<Integer, ClientRoomInfo> activeRoom;
}
