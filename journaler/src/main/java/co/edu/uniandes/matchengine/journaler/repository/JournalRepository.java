package co.edu.uniandes.matchengine.journaler.repository;

import co.edu.uniandes.matchengine.journaler.model.JournalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalRepository extends JpaRepository<JournalEntity, Long> {
}