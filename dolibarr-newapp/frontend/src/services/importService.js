import { envoyerRequete } from './apiService.js';

const CLE_BACKOFFICE = 'codeBackofficeValide';

/** En-têtes communs aux requêtes d'import : code backoffice, jamais la clé API Dolibarr. */
function entetesBackoffice() {
  return {
    'X-BACKOFFICE-CODE': sessionStorage.getItem(CLE_BACKOFFICE),
  };
}

/** Retire les champs internes de prévisualisation avant l'envoi au backend. */
function nettoyer(ligne) {
  const { ligne: _numeroLigne, ...donnees } = ligne;
  return donnees;
}

/** Envoie les employés validés au backend (création via l'API users de Dolibarr). */
export async function importerEmployes(employesValides) {
  const employes = employesValides.map(nettoyer);

  return envoyerRequete('/api/import/employes', 'POST', { employes }, {
    headers: entetesBackoffice(),
  });
}

/** Envoie les salaires validés au backend (résolution ref_employe + création salaires/paiements). */
export async function importerSalaires(salairesValides) {
  const salaires = salairesValides.map(nettoyer);

  return envoyerRequete('/api/import/salaires', 'POST', { salaires }, {
    headers: entetesBackoffice(),
  });
}

/** Envoie les photos extraites au backend (upload Dolibarr + miniatures, résolution ref_employe). */
export async function importerPhotos(photosValides) {
  // On n'envoie que ce dont le backend a besoin : ref_employe, nom de fichier et contenu base64.
  const photos = photosValides.map((photo) => ({
    refEmploye: photo.ref,
    nomFichier: photo.nomFichier,
    contenuBase64: photo.base64,
  }));

  return envoyerRequete('/api/import/photos', 'POST', { photos }, {
    headers: entetesBackoffice(),
  });
}
