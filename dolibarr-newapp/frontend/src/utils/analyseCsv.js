import Papa from 'papaparse';

// Colonnes attendues dans chaque fichier CSV.
const COLONNES_EMPLOYES = ['ref_employe', 'nom', 'genre', 'identifiant', 'mdp', 'heure_travail_semaine', 'poste'];
const COLONNES_SALAIRES = ['ref_salaire', 'ref_employe', 'date_debut', 'date_fin', 'montant', 'paiement'];

/**
 * Convertit un montant au format français ("677,56" ou "1 234,56") en nombre décimal.
 * Renvoie NaN si la valeur n'est pas convertible.
 */
export function convertirMontant(valeur) {
  if (valeur === null || valeur === undefined || String(valeur).trim() === '') {
    return NaN;
  }

  let texte = String(valeur).replace(/\s| /g, '');
  if (texte.includes(',')) {
    // Virgule = séparateur décimal FR ; le point éventuel est un séparateur de milliers.
    texte = texte.replace(/\./g, '').replace(',', '.');
  }

  return Number.parseFloat(texte);
}

/**
 * Convertit le genre français en valeur anglaise attendue par Dolibarr.
 * Renvoie null si le genre n'est pas reconnu.
 */
export function convertirGenre(valeur) {
  const genre = String(valeur ?? '').trim().toLowerCase();
  if (['homme', 'h', 'm', 'man', 'masculin'].includes(genre)) {
    return 'man';
  }
  if (['femme', 'f', 'woman', 'feminin', 'féminin'].includes(genre)) {
    return 'woman';
  }
  return null;
}

/**
 * Parse le champ "paiement" au format pseudo-JSON {["08/03/26",890],["08/03/26",300]}
 * en tableau d'objets { date, montant }. Tolère un champ vide (aucun paiement effectué).
 * La syntaxe n'étant pas du JSON strict, on extrait chaque couple [date, montant] par regex.
 */
export function analyserPaiements(valeur) {
  const texte = String(valeur ?? '').trim();
  if (texte === '' || texte === '{}') {
    return { paiements: [], erreur: null };
  }

  const paiements = [];
  const motif = /\[\s*"?([^",\]]+)"?\s*,\s*"?([^",\]]+)"?\s*\]/g;
  let correspondance;

  while ((correspondance = motif.exec(texte)) !== null) {
    const date = correspondance[1].trim();
    const montant = convertirMontant(correspondance[2]);

    if (Number.isNaN(montant)) {
      return { paiements: [], erreur: `Montant de paiement invalide : "${correspondance[2]}"` };
    }

    paiements.push({ date, montant });
  }

  if (paiements.length === 0) {
    return { paiements: [], erreur: `Champ paiement illisible : "${texte}"` };
  }

  return { paiements, erreur: null };
}

/** Vérifie que toutes les colonnes attendues sont présentes dans l'en-tête du CSV. */
function colonnesManquantes(champs, colonnesAttendues) {
  const presents = (champs || []).map((champ) => champ.trim());
  return colonnesAttendues.filter((colonne) => !presents.includes(colonne));
}

/** Lance papaparse sur un fichier et renvoie une promesse avec les lignes brutes. */
function lireCsv(fichier) {
  return new Promise((resoudre, rejeter) => {
    Papa.parse(fichier, {
      header: true,
      skipEmptyLines: true,
      transformHeader: (entete) => entete.trim(),
      complete: (resultat) => resoudre(resultat),
      error: (erreur) => rejeter(erreur),
    });
  });
}

/**
 * Parse et valide le fichier des employés.
 * Renvoie { valides, erreurs } : `valides` est prêt à être envoyé au backend (genre converti),
 * `erreurs` détaille les lignes rejetées pour la prévisualisation.
 */
export async function analyserFichierEmployes(fichier) {
  const resultat = await lireCsv(fichier);

  const manquantes = colonnesManquantes(resultat.meta.fields, COLONNES_EMPLOYES);
  if (manquantes.length > 0) {
    return { valides: [], erreurs: [], colonnesManquantes: manquantes };
  }

  const valides = [];
  const erreurs = [];

  resultat.data.forEach((ligne, index) => {
    const numeroLigne = index + 2; // +1 en-tête, +1 base 1
    const raisons = [];

    const refEmploye = (ligne.ref_employe || '').trim();
    const nom = (ligne.nom || '').trim();
    const identifiant = (ligne.identifiant || '').trim();
    const mdp = ligne.mdp ?? '';
    const genre = convertirGenre(ligne.genre);
    const heureBrute = (ligne.heure_travail_semaine || '').trim();
    const poste = (ligne.poste || '').trim();

    if (!refEmploye) raisons.push('ref_employe manquant');
    if (!nom) raisons.push('nom manquant');
    if (!identifiant) raisons.push('identifiant manquant');
    if (String(mdp).trim() === '') raisons.push('mdp manquant');
    if (genre === null) raisons.push(`genre non reconnu : "${ligne.genre ?? ''}"`);

    let heureTravailSemaine = null;
    if (heureBrute !== '') {
      const heure = convertirMontant(heureBrute);
      if (Number.isNaN(heure)) {
        raisons.push(`heure_travail_semaine invalide : "${heureBrute}"`);
      } else {
        heureTravailSemaine = heure;
      }
    }

    if (raisons.length > 0) {
      erreurs.push({ ligne: numeroLigne, raisons, brut: ligne });
      return;
    }

    valides.push({
      ligne: numeroLigne,
      refEmploye,
      nom,
      genre,
      identifiant,
      mdp,
      heureTravailSemaine,
      poste,
    });
  });

  return { valides, erreurs, colonnesManquantes: [] };
}

/**
 * Parse et valide le fichier des salaires.
 * Renvoie { valides, erreurs } : `valides` est prêt à être envoyé au backend (montant et
 * paiements convertis), `erreurs` détaille les lignes rejetées pour la prévisualisation.
 */
export async function analyserFichierSalaires(fichier) {
  const resultat = await lireCsv(fichier);

  const manquantes = colonnesManquantes(resultat.meta.fields, COLONNES_SALAIRES);
  if (manquantes.length > 0) {
    return { valides: [], erreurs: [], colonnesManquantes: manquantes };
  }

  const valides = [];
  const erreurs = [];

  resultat.data.forEach((ligne, index) => {
    const numeroLigne = index + 2;
    const raisons = [];

    const refSalaire = (ligne.ref_salaire || '').trim();
    const refEmploye = (ligne.ref_employe || '').trim();
    const dateDebut = (ligne.date_debut || '').trim();
    const dateFin = (ligne.date_fin || '').trim();
    const montant = convertirMontant(ligne.montant);
    const { paiements, erreur: erreurPaiement } = analyserPaiements(ligne.paiement);

    if (!refSalaire) raisons.push('ref_salaire manquant');
    if (!refEmploye) raisons.push('ref_employe manquant');
    if (!dateDebut) raisons.push('date_debut manquante');
    if (!dateFin) raisons.push('date_fin manquante');
    if (Number.isNaN(montant)) raisons.push(`montant invalide : "${ligne.montant ?? ''}"`);
    if (erreurPaiement) raisons.push(erreurPaiement);

    if (raisons.length > 0) {
      erreurs.push({ ligne: numeroLigne, raisons, brut: ligne });
      return;
    }

    valides.push({
      ligne: numeroLigne,
      refSalaire,
      refEmploye,
      dateDebut,
      dateFin,
      montant,
      paiements,
    });
  });

  return { valides, erreurs, colonnesManquantes: [] };
}
