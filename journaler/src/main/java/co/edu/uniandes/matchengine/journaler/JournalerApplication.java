package co.edu.uniandes.matchengine.journaler; // Identidad propia

import co.edu.uniandes.matchengine.apihandler.dto.OrdenDTO;
import co.edu.uniandes.matchengine.journaler.repository.JournalRepository;
import co.edu.uniandes.matchengine.journaler.model.JournalEntity;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import java.util.function.Consumer;

@SpringBootApplication
public class JournalerApplication {

    public static void main(String[] args) {
        SpringApplication.run(JournalerApplication.class, args);
    }

    @Bean
    public Consumer<OrdenDTO> registrarOrden(JournalRepository repository) {
        return orden -> {
            long ts_registro = System.nanoTime();
            JournalEntity entity = new JournalEntity(
                orden.getId(),
                orden.getProductoId(),
                orden.getCantidad(),
                orden.getTipo(),
                orden.getTs_api_recepcion(),
                orden.getTs_api_salida(),
                ts_registro
            );
            repository.save(entity);
            System.out.println("Journaler: Orden " + orden.getId() + " persistida.");
        };
    }
}