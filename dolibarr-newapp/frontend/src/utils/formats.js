// Formatage partagé pour le front-office.

/** Formate un montant en français avec le suffixe « Ar » (677.56 -> "677,56 Ar"). */
export function formaterMontant(montant) {
  const valeur = new Intl.NumberFormat('fr-FR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(Number(montant || 0));
  return `${valeur} Ar`;
}

/** Convertit le genre Dolibarr (man/woman) en libellé français pour l'affichage. */
export function libelleGenre(genre) {
  if (genre === 'man') return 'Homme';
  if (genre === 'woman') return 'Femme';
  return '—';
}

/** Convertit une date ISO yyyy-MM-dd en jj/mm/aaaa ; renvoie l'entrée si non parsable. */
export function formaterDate(dateIso) {
  if (!dateIso) return '—';
  const correspondance = /^(\d{4})-(\d{2})-(\d{2})/.exec(dateIso);
  return correspondance ? `${correspondance[3]}/${correspondance[2]}/${correspondance[1]}` : dateIso;
}

/** Représentation visuelle d'un statut de salaire (code backend -> libellé + classe + icône). */
export function descriptionStatut(statut) {
  if (statut === 'solde') return { libelle: 'Soldé', classe: 'statut--solde', icone: '✅' };
  if (statut === 'partiel') return { libelle: 'Partiellement payé', classe: 'statut--partiel', icone: '⚠️' };
  return { libelle: 'Impayé', classe: 'statut--impaye', icone: '❌' };
}
