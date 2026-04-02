package com.uniandes.ventas.persistence.entity;

import com.uniandes.ventas.domain.model.OrdenCompraStatus;
import com.uniandes.ventas.web.dto.OrdenCompraRequest;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orden_compra", schema = "public",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ordencompra_idFactura", columnNames = "id_factura")
        }
)

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // requerido por JPA
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true) // evita imprimir todo (lazy, relaciones)
@EqualsAndHashCode(onlyExplicitlyIncluded = true) // equals/hashCode por id

public class OrdenCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_orden_compra")
    @ToString.Include
    private Long idOrdenCompra;

    @Column(name = "id_cliente", nullable = false)
    @ToString.Include
    private Integer idCliente;

    @Column(name = "id_factura", length = 120, nullable = false, unique = true)
    @ToString.Include
    private String idFactura;

    @Column(name = "fecha_compra", nullable = false)
    @ToString.Include
    private LocalDateTime fechaCompra;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 50, nullable = false)
    @ToString.Include
    private OrdenCompraStatus estado;

    @Column(name = "total", precision = 18, scale = 2, nullable = false)
    private BigDecimal total;

    @Builder
    public OrdenCompra(String idFactura, Integer idCliente, LocalDateTime fechaCompra,
                       OrdenCompraStatus estado, BigDecimal total, Long idOrdenCompra) {
        this.idFactura = idFactura;
        this.idCliente = idCliente;
        this.fechaCompra = fechaCompra;
        this.estado = estado;
        this.total = total;
        this.idOrdenCompra = idOrdenCompra;
    }

    public OrdenCompra(OrdenCompraRequest request) {
        this.idFactura = request.getIdFactura();
        this.idCliente = request.getIdCliente();
        this.fechaCompra = request.getFechaCompra();
        this.estado = request.getEstado();
        this.total = request.getTotal();
    }
}
