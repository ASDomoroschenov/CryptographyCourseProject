package ru.mai.javachatservice.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.mai.javachatservice.model.Room;

@Repository
public interface RoomRepository extends CrudRepository<Room, Long> {

}
