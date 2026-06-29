import JSZip from 'jszip';

// Extensions d'image acceptées dans le zip.
const EXTENSIONS_IMAGE = ['png', 'jpg', 'jpeg', 'gif', 'webp'];

/** Déduit le type MIME à partir de l'extension, pour l'aperçu (data URL). */
function typeMime(extension) {
  if (extension === 'jpg' || extension === 'jpeg') return 'image/jpeg';
  if (extension === 'gif') return 'image/gif';
  if (extension === 'webp') return 'image/webp';
  return 'image/png';
}

/**
 * Extrait les images d'un fichier .zip côté client.
 * Le ref_employe est déduit du nom de fichier sans extension (ex : "1.png" -> "1").
 * Chaque image valide est renvoyée avec son contenu base64 (pour l'envoi au backend) et
 * une data URL (pour l'aperçu). Les fichiers non-image sont remontés en erreurs.
 *
 * @param {File} fichier            le zip déposé
 * @param {Set<string>} refsConnues refs employés du fichier Feuille_1 (pour repérer les orphelines)
 * @returns {{ valides: Array, erreurs: Array }}
 */
export async function analyserZipPhotos(fichier, refsConnues = new Set()) {
  const zip = await JSZip.loadAsync(fichier);
  const valides = [];
  const erreurs = [];

  const entrees = Object.values(zip.files).filter((entree) => !entree.dir);

  for (const entree of entrees) {
    const nomFichier = entree.name.split('/').pop(); // ignore l'arborescence interne du zip
    if (!nomFichier || nomFichier.startsWith('.')) {
      continue;
    }

    const point = nomFichier.lastIndexOf('.');
    const extension = point >= 0 ? nomFichier.slice(point + 1).toLowerCase() : '';
    const ref = point >= 0 ? nomFichier.slice(0, point) : nomFichier;

    if (!EXTENSIONS_IMAGE.includes(extension)) {
      erreurs.push({ nomFichier, raison: `Fichier non-image ignoré (.${extension || 'sans extension'})` });
      continue;
    }

    const base64 = await entree.async('base64');
    // Orpheline si le ref n'existe pas dans les employés chargés (détection seulement si on les connaît).
    const orpheline = refsConnues.size > 0 && !refsConnues.has(ref);

    valides.push({
      ref,
      nomFichier,
      base64,
      apercu: `data:${typeMime(extension)};base64,${base64}`,
      orpheline,
    });
  }

  valides.sort((a, b) => a.ref.localeCompare(b.ref, undefined, { numeric: true }));

  return { valides, erreurs };
}
