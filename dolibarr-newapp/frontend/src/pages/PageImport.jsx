import { useState } from 'react';
import ActionButton from '../components/ActionButton.jsx';
import ZoneImportCsv from '../components/ZoneImportCsv.jsx';
import ZoneImportPhotos from '../components/ZoneImportPhotos.jsx';
import { analyserFichierEmployes, analyserFichierSalaires } from '../utils/analyseCsv.js';
import { analyserZipPhotos } from '../utils/analyseZipPhotos.js';
import { importerEmployes, importerPhotos, importerSalaires } from '../services/importService.js';

// Colonnes affichées dans les tableaux de prévisualisation.
const COLONNES_EMPLOYES = [
  { cle: 'refEmploye', libelle: 'Réf.' },
  { cle: 'nom', libelle: 'Nom' },
  { cle: 'genre', libelle: 'Genre' },
  { cle: 'identifiant', libelle: 'Identifiant' },
  { cle: 'mdp', libelle: 'Mot de passe', formater: () => '••••••' },
  { cle: 'heureTravailSemaine', libelle: 'Heures/sem.', formater: (valeur) => valeur ?? '—' },
  { cle: 'poste', libelle: 'Poste', formater: (valeur) => valeur || '—' },
];

const COLONNES_SALAIRES = [
  { cle: 'refSalaire', libelle: 'Réf.' },
  { cle: 'refEmploye', libelle: 'Réf. employé' },
  { cle: 'dateDebut', libelle: 'Début' },
  { cle: 'dateFin', libelle: 'Fin' },
  { cle: 'montant', libelle: 'Montant', formater: (valeur) => formaterMontant(valeur) },
  { cle: 'paiements', libelle: 'Paiements', formater: (paiements) => `${paiements.length} paiement(s)` },
];

function formaterMontant(montant) {
  return new Intl.NumberFormat('fr-FR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(Number(montant || 0));
}

export default function PageImport() {
  const [analyseEmployes, setAnalyseEmployes] = useState(null);
  const [analyseSalaires, setAnalyseSalaires] = useState(null);
  const [analysePhotos, setAnalysePhotos] = useState(null);
  const [rapportEmployes, setRapportEmployes] = useState(null);
  const [rapportSalaires, setRapportSalaires] = useState(null);
  const [rapportPhotos, setRapportPhotos] = useState(null);
  const [chargement, setChargement] = useState(false);
  const [erreur, setErreur] = useState('');

  const nbEmployes = analyseEmployes?.valides?.length ?? 0;
  const nbSalaires = analyseSalaires?.valides?.length ?? 0;
  const nbPhotos = analysePhotos?.valides?.length ?? 0;
  const peutImporter = nbEmployes > 0 || nbSalaires > 0 || nbPhotos > 0;

  // Refs employés chargés, pour repérer les photos orphelines à l'affichage.
  const refsEmployes = new Set((analyseEmployes?.valides ?? []).map((employe) => employe.refEmploye));

  // Import séquentiel : employés d'abord (salaires et photos référencent ref_employe via le tracking).
  async function lancerImport() {
    setChargement(true);
    setErreur('');
    setRapportEmployes(null);
    setRapportSalaires(null);
    setRapportPhotos(null);

    try {
      if (nbEmployes > 0) {
        setRapportEmployes(await importerEmployes(analyseEmployes.valides));
      }
      if (nbSalaires > 0) {
        setRapportSalaires(await importerSalaires(analyseSalaires.valides));
      }
      if (nbPhotos > 0) {
        setRapportPhotos(await importerPhotos(analysePhotos.valides));
      }
    } catch (err) {
      setErreur(err.message || "Échec de l'import");
    } finally {
      setChargement(false);
    }
  }

  return (
    <div className="page-inner">
      <div className="page-inner-header">
        <h1>Import</h1>
        <p>Peupler Dolibarr depuis deux fichiers CSV : employés puis salaires</p>
      </div>

      <ZoneImportCsv
        titre="Fichier Employés (Feuille_1)"
        description="Colonnes : ref_employe, nom, genre, identifiant, mdp, heure_travail_semaine, poste"
        colonnesApercu={COLONNES_EMPLOYES}
        onAnalyser={analyserFichierEmployes}
        onAnalyseChange={setAnalyseEmployes}
        rapport={rapportEmployes}
        desactive={chargement}
      />

      <ZoneImportCsv
        titre="Fichier Salaires (Feuille_2)"
        description="Colonnes : ref_salaire, ref_employe, date_debut, date_fin, montant, paiement"
        colonnesApercu={COLONNES_SALAIRES}
        onAnalyser={analyserFichierSalaires}
        onAnalyseChange={setAnalyseSalaires}
        rapport={rapportSalaires}
        desactive={chargement}
      />

      <ZoneImportPhotos
        onAnalyser={(fichier) => analyserZipPhotos(fichier, refsEmployes)}
        onAnalyseChange={setAnalysePhotos}
        refsEmployes={refsEmployes}
        rapport={rapportPhotos}
        desactive={chargement}
      />

      <div className="import-barre-action">
        <p className="import-info">
          Import séquentiel : {nbEmployes} employé(s), puis {nbSalaires} salaire(s), puis {nbPhotos} photo(s).
        </p>
        <ActionButton variant="primary" disabled={!peutImporter || chargement} onClick={lancerImport}>
          {chargement ? 'Import en cours...' : 'Importer'}
        </ActionButton>
      </div>

      {erreur && <p className="message message--error">{erreur}</p>}
    </div>
  );
}
