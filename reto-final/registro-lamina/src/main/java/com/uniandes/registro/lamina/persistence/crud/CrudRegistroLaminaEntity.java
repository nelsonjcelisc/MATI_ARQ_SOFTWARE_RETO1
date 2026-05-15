package com.uniandes.registro.lamina.persistence.crud;

import com.uniandes.registro.lamina.persistence.entity.RegistroLamina;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CrudRegistroLaminaEntity extends JpaRepository<RegistroLamina, Long> {

    Optional<RegistroLamina> findByIdLamina(String idLamina);
}
