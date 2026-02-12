package co.edu.uniandes.matchengine.journaler.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "journal_ordenes")
public class JournalEntity {
    @Id
    private Long id;
    private Long productoId;
    private Integer cantidad;
    private Byte tipo;
    private Long tsApiRecepcion;
    private Long tsApiSalida;
    private Long tsJournalRegistro;

    public JournalEntity() {}

    public JournalEntity(Long id, Long productoId, Integer cantidad, Byte tipo, 
                         Long tsApiRecepcion, Long tsApiSalida, Long tsJournalRegistro) {
        this.id = id;
        this.productoId = productoId;
        this.cantidad = cantidad;
        this.tipo = tipo;
        this.tsApiRecepcion = tsApiRecepcion;
        this.tsApiSalida = tsApiSalida;
        this.tsJournalRegistro = tsJournalRegistro;
    }
}