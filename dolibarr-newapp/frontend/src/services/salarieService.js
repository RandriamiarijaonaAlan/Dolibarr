import { envoyerRequete } from './apiService.js';

export async function recupererSalariesFrontoffice() {
  return envoyerRequete('/api/frontoffice/salaries');
}

export async function recupererDetailSalarieFrontoffice(id) {
  return envoyerRequete(`/api/frontoffice/salaries/${id}`);
}
