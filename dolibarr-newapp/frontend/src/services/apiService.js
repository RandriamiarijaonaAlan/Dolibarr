const API_BASE_URL = 'http://localhost:8080';

// URL absolue du backend, utile pour pointer un <img src> directement vers une route proxy.
export const URL_BASE_API = API_BASE_URL;

export async function envoyerRequete(chemin, methode = 'GET', donnees = null, options = {}) {
  const configuration = {
    method: methode,
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
  };

  if (donnees) {
    configuration.body = JSON.stringify(donnees);
  }

  const reponse = await fetch(`${API_BASE_URL}${chemin}`, configuration);
  const contenu = await lireReponseJson(reponse);

  if (!reponse.ok) {
    throw new Error(contenu?.message || `Erreur HTTP ${reponse.status}`);
  }

  return contenu;
}

async function lireReponseJson(reponse) {
  const texte = await reponse.text();

  if (!texte) {
    return null;
  }

  return JSON.parse(texte);
}
