import { envoyerRequete } from './apiService.js';

const CLE_BACKOFFICE = 'codeBackofficeValide';

function entetesBackoffice() {
  return {
    'X-BACKOFFICE-CODE': sessionStorage.getItem(CLE_BACKOFFICE),
  };
}

export async function listerJoursFeries() {
  return envoyerRequete('/api/backoffice/jours-feries');
}

export async function creerJourFerie(jourFerie) {
  return envoyerRequete('/api/backoffice/jours-feries', 'POST', jourFerie, {
    headers: entetesBackoffice(),
  });
}

export async function mettreAJourJourFerie(id, jourFerie) {
  return envoyerRequete(`/api/backoffice/jours-feries/${id}`, 'PUT', jourFerie, {
    headers: entetesBackoffice(),
  });
}

export async function effacerJourFerie(id) {
  return envoyerRequete(`/api/backoffice/jours-feries/${id}`, 'DELETE', null, {
    headers: entetesBackoffice(),
  });
}
