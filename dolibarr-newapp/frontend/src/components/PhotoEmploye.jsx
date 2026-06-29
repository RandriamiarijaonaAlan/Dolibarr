import { useState } from 'react';
import { urlPhotoEmploye } from '../services/frontofficeService.js';

/**
 * Photo d'un employé (miniature servie par le backend proxy). En l'absence de photo
 * ou en cas d'erreur de chargement, affiche l'initiale du nom comme repli.
 */
export default function PhotoEmploye({ id, nom, taille = 'moyenne' }) {
  const [erreur, setErreur] = useState(false);

  if (erreur || !id) {
    return (
      <div className={`photo-employe photo-employe--${taille} photo-employe--vide`}>
        {(nom || '?').charAt(0).toUpperCase()}
      </div>
    );
  }

  return (
    <img
      className={`photo-employe photo-employe--${taille}`}
      src={urlPhotoEmploye(id)}
      alt={nom || 'Photo'}
      onError={() => setErreur(true)}
    />
  );
}
