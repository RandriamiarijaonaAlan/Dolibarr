package com.newapp.dolibarr.repository;

import com.newapp.dolibarr.model.JourFerie;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JourFerieRepository extends JpaRepository<JourFerie, Long> {

    boolean existsByDateJourAndNomIgnoreCase(LocalDate dateJour, String nom);

    boolean existsByDateJourAndNomIgnoreCaseAndIdNot(LocalDate dateJour, String nom, Long id);
}
