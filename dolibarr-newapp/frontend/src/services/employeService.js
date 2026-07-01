import { envoyerRequete } from './apiService.js';

export async function rechercherEmployes(filtres = {}) {
  const parametres = new URLSearchParams();

  if (filtres.poste) parametres.set('poste', filtres.poste);
  if (filtres.genre && filtres.genre !== 'tous') parametres.set('genre', filtres.genre);
  if (filtres.heureMin !== '') parametres.set('heureMin', filtres.heureMin);
  if (filtres.heureMax !== '') parametres.set('heureMax', filtres.heureMax);

  const query = parametres.toString();
  return envoyerRequete(`/api/frontoffice/employes${query ? `?${query}` : ''}`);
}
