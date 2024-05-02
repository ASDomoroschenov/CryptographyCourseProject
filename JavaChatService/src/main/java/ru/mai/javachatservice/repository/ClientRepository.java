package ru.mai.javachatservice.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.mai.javachatservice.model.Client;

@Repository
public interface ClientRepository extends CrudRepository<Client, Long> {
    @Transactional
    default Client addRoom(long clientId, long roomId) {
        Client client = findById(clientId).orElse(null);

        if (client != null) {
            long[] updatedRooms = new long[client.getRooms().length + 1];
            System.arraycopy(client.getRooms(), 0, updatedRooms, 0, client.getRooms().length);
            updatedRooms[updatedRooms.length - 1] = roomId;
            client.setRooms(updatedRooms);
            return save(client);
        } else {
            return null;
        }
    }
}
