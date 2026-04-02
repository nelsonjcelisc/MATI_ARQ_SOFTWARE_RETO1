package co.edu.uniandes.matchengine.journaler.repository;

import co.edu.uniandes.matchengine.journaler.model.JournalEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface JournalRepository extends MongoRepository<JournalEntity, Long> {
}

// package co.edu.uniandes.matchengine.journaler.repository;

// import co.edu.uniandes.matchengine.journaler.model.JournalEntity;
// import org.springframework.data.jpa.repository.JpaRepository;

// public interface JournalRepository extends JpaRepository<JournalEntity, Long> {
// }