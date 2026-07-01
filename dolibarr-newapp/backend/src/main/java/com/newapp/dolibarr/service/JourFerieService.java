package com.newapp.dolibarr.service;

import com.newapp.dolibarr.dto.JourFerieDto;
import com.newapp.dolibarr.model.JourFerie;
import com.newapp.dolibarr.repository.JourFerieRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class JourFerieService {

    private final JourFerieRepository jourFerieRepository;

    public JourFerieService(JourFerieRepository jourFerieRepository) {
        this.jourFerieRepository = jourFerieRepository;
    }

    public List<JourFerieDto> lister() {
        return jourFerieRepository.findAll().stream()
                .map(this::versDto)
                .toList();
    }

    public JourFerieDto trouver(Long id) {
        return versDto(trouverEntite(id));
    }

    public JourFerieDto creer(JourFerieDto dto) {
        valider(dto);
        if (jourFerieRepository.existsByDateJourAndNomIgnoreCase(dto.dateJour(), dto.nom().trim())) {
            throw new IllegalArgumentException("Un jour férié existe déjà avec ce nom et cette date");
        }

        JourFerie jourFerie = new JourFerie();
        appliquer(dto, jourFerie);
        return versDto(jourFerieRepository.save(jourFerie));
    }

    public JourFerieDto modifier(Long id, JourFerieDto dto) {
        valider(dto);
        JourFerie jourFerie = trouverEntite(id);
        if (jourFerieRepository.existsByDateJourAndNomIgnoreCaseAndIdNot(dto.dateJour(), dto.nom().trim(), id)) {
            throw new IllegalArgumentException("Un jour férié existe déjà avec ce nom et cette date");
        }

        appliquer(dto, jourFerie);
        return versDto(jourFerieRepository.save(jourFerie));
    }

    public void supprimer(Long id) {
        JourFerie jourFerie = trouverEntite(id);
        jourFerieRepository.delete(jourFerie);
    }

    private JourFerie trouverEntite(Long id) {
        return jourFerieRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Jour férié introuvable"));
    }

    private void valider(JourFerieDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Données du jour férié manquantes");
        }
        if (dto.nom() == null || dto.nom().isBlank()) {
            throw new IllegalArgumentException("Le nom du jour férié est obligatoire");
        }
        if (dto.dateJour() == null) {
            throw new IllegalArgumentException("La date du jour férié est obligatoire");
        }
    }

    private void appliquer(JourFerieDto dto, JourFerie jourFerie) {
        jourFerie.setNom(dto.nom().trim());
        jourFerie.setDateJour(dto.dateJour());
        jourFerie.setDescription(dto.description() == null ? null : dto.description().trim());
        jourFerie.setActif(dto.actif() == null || dto.actif());
    }

    private JourFerieDto versDto(JourFerie jourFerie) {
        return new JourFerieDto(
                jourFerie.getId(),
                jourFerie.getNom(),
                jourFerie.getDateJour(),
                jourFerie.getDescription(),
                jourFerie.getActif()
        );
    }
}
