import { URL_BASE_API, envoyerRequete } from './apiService.js';

/** Liste des salariés avec leurs agrégats (nb salaires, montant total, reste à payer). */
export async function recupererEmployes() {
  return envoyerRequete('/api/frontoffice/employes');
}

/** Salaires d'un employé donné. */
export async function recupererSalairesEmploye(idEmploye) {
  return envoyerRequete(`/api/frontoffice/employes/${idEmploye}/salaires`);
}

/** Détail d'un salaire (infos employé, historique des paiements, reste à payer, statut). */
export async function recupererSalaire(idSalaire) {
  return envoyerRequete(`/api/frontoffice/salaires/${idSalaire}`);
}

/** Crée un salaire ; renvoie { success, message, id }. */
export async function creerSalaire(donnees) {
  return envoyerRequete('/api/frontoffice/salaires', 'POST', donnees);
}

/** Ajoute un paiement partiel sur un salaire ; renvoie { success, message, id }. */
export async function ajouterPaiement(idSalaire, donnees) {
  return envoyerRequete(`/api/frontoffice/salaires/${idSalaire}/paiements`, 'POST', donnees);
}

/** Modes de paiement disponibles (id fk_typepayment + libellé). */
export async function recupererModesPaiement() {
  return envoyerRequete('/api/frontoffice/modes-paiement');
}

/** URL absolue de la photo d'un employé (route proxy backend, clé API jamais exposée). */
export function urlPhotoEmploye(idEmploye) {
  return `${URL_BASE_API}/api/frontoffice/employes/${idEmploye}/photo`;
}
