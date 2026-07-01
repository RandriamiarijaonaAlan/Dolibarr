import { envoyerRequete } from './apiService.js';

export async function genererSalaires(donnees) {
  return envoyerRequete('/api/frontoffice/salaires/generer', 'POST', donnees);
}
