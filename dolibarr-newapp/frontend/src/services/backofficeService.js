import { envoyerRequete } from './apiService.js';

const CLE_BACKOFFICE = 'codeBackofficeValide';
const CODE_UNIQUE_PAR_DEFAUT = 'BACKOFFICE-2026';

export function obtenirCodeUniqueParDefaut() {
  return CODE_UNIQUE_PAR_DEFAUT;
}

export function estBackofficeAutorise() {
  return Boolean(sessionStorage.getItem(CLE_BACKOFFICE));
}

export function oublierCodeBackoffice() {
  sessionStorage.removeItem(CLE_BACKOFFICE);
}

export async function verifierCode(code) {
  const resultat = await envoyerRequete('/api/backoffice/check-code', 'POST', { code });

  if (resultat.autorise) {
    sessionStorage.setItem(CLE_BACKOFFICE, code);
  }

  return resultat;
}

export async function reinitialiserDonnees() {
  const code = sessionStorage.getItem(CLE_BACKOFFICE);

  return envoyerRequete('/api/backoffice/reset-data', 'POST', null, {
    headers: {
      'X-BACKOFFICE-CODE': code,
    },
  });
}
