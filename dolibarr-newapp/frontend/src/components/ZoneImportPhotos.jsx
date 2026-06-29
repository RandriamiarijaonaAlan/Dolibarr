import { useRef, useState } from 'react';

/**
 * Zone de dépôt d'un fichier .zip de photos, avec extraction et aperçu côté client.
 * Chaque image est associée à un ref_employe (déduit du nom de fichier) ; les photos dont
 * le ref n'existe pas dans les employés chargés sont signalées comme orphelines.
 * L'upload réel est déclenché par le bouton global de la page (cette zone n'a pas de bouton).
 *
 * Props :
 *  - onAnalyser(fichier) -> { valides, erreurs }  (extraction du zip)
 *  - onAnalyseChange(resultat|null) : remonte l'analyse au parent
 *  - refsEmployes : Set des refs employés chargés (pour repérer les orphelines à l'affichage)
 *  - rapport : rapport d'import renvoyé par le backend
 *  - desactive : verrou pendant un import en cours
 */
export default function ZoneImportPhotos({
  onAnalyser,
  onAnalyseChange,
  refsEmployes = new Set(),
  rapport,
  desactive = false,
}) {
  const champFichier = useRef(null);
  const [nomFichier, setNomFichier] = useState('');
  const [survol, setSurvol] = useState(false);
  const [analyse, setAnalyse] = useState(null);
  const [erreurAnalyse, setErreurAnalyse] = useState('');

  function reinitialiser() {
    setNomFichier('');
    setAnalyse(null);
    setErreurAnalyse('');
    onAnalyseChange?.(null);
    if (champFichier.current) {
      champFichier.current.value = '';
    }
  }

  async function traiterFichier(fichier) {
    if (!fichier) return;

    if (!fichier.name.toLowerCase().endsWith('.zip')) {
      reinitialiser();
      setNomFichier(fichier.name);
      setErreurAnalyse('Format attendu : un fichier .zip');
      return;
    }

    reinitialiser();
    setNomFichier(fichier.name);

    try {
      const resultat = await onAnalyser(fichier);
      setAnalyse(resultat);
      onAnalyseChange?.(resultat);
    } catch (err) {
      setErreurAnalyse(err.message || 'Lecture du zip impossible');
    }
  }

  function gererDepot(event) {
    event.preventDefault();
    setSurvol(false);
    if (desactive) return;
    traiterFichier(event.dataTransfer.files?.[0]);
  }

  function estOrpheline(ref) {
    return refsEmployes.size > 0 && !refsEmployes.has(ref);
  }

  const nbOrphelines = analyse?.valides?.filter((photo) => estOrpheline(photo.ref)).length ?? 0;

  return (
    <section className="import-zone">
      <div className="import-zone-entete">
        <h2>Fichier Photos (images.zip)</h2>
        <p>Archive .zip d'images nommées par ref_employe (ex : 1.png, 2.png). Importées après les employés.</p>
      </div>

      <div
        className={'import-depot' + (survol ? ' import-depot--survol' : '')}
        onClick={() => !desactive && champFichier.current?.click()}
        onDragOver={(event) => {
          event.preventDefault();
          if (!desactive) setSurvol(true);
        }}
        onDragLeave={() => setSurvol(false)}
        onDrop={gererDepot}
      >
        <input
          ref={champFichier}
          type="file"
          accept=".zip,application/zip,application/x-zip-compressed"
          className="import-depot-input"
          disabled={desactive}
          onChange={(event) => traiterFichier(event.target.files?.[0])}
        />
        <span className="import-depot-icone">🗜️</span>
        <span>{nomFichier || 'Glisser images.zip ici ou cliquer pour parcourir'}</span>
      </div>

      {erreurAnalyse && <p className="message message--error">{erreurAnalyse}</p>}

      {analyse && (
        <div className="import-apercu">
          <div className="import-apercu-resume">
            <span className="import-badge import-badge--ok">{analyse.valides.length} image(s)</span>
            {nbOrphelines > 0 && (
              <span className="import-badge import-badge--erreur">{nbOrphelines} orpheline(s)</span>
            )}
            {analyse.erreurs.length > 0 && (
              <span className="import-badge import-badge--erreur">{analyse.erreurs.length} fichier(s) ignoré(s)</span>
            )}
            <button className="import-lien-annuler" disabled={desactive} onClick={reinitialiser}>
              Annuler
            </button>
          </div>

          {analyse.valides.length > 0 && (
            <div className="import-grille-photos">
              {analyse.valides.map((photo) => (
                <figure
                  key={photo.nomFichier}
                  className={'import-vignette' + (estOrpheline(photo.ref) ? ' import-vignette--orpheline' : '')}
                >
                  <img src={photo.apercu} alt={photo.nomFichier} />
                  <figcaption>
                    ref {photo.ref}
                    {estOrpheline(photo.ref) && <span className="import-tag-orphelin">orpheline</span>}
                  </figcaption>
                </figure>
              ))}
            </div>
          )}

          {analyse.erreurs.length > 0 && (
            <div className="import-erreurs">
              <h3>Fichiers ignorés</h3>
              <ul>
                {analyse.erreurs.map((erreur) => (
                  <li key={erreur.nomFichier}>
                    <strong>{erreur.nomFichier}</strong> : {erreur.raison}
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}

      {rapport && (
        <div className="import-rapport">
          <h3>Rapport d'import</h3>
          <p className={'message' + (rapport.success ? '' : ' message--error')}>{rapport.message}</p>
          <div className="import-apercu-resume">
            <span className="import-badge import-badge--ok">{rapport.lignesImportees} importée(s)</span>
            <span className="import-badge import-badge--erreur">{rapport.lignesEnErreur} en erreur</span>
          </div>

          {rapport.resultats?.length > 0 && (
            <div className="table-wrapper">
              <table className="stat-table">
                <thead>
                  <tr>
                    <th>Réf. employé</th>
                    <th>Statut</th>
                    <th>ID Dolibarr</th>
                    <th>Détail photo</th>
                  </tr>
                </thead>
                <tbody>
                  {rapport.resultats.map((resultat, index) => (
                    <tr key={`${resultat.reference}-${index}`}>
                      <td>{resultat.reference}</td>
                      <td>{resultat.succes ? '✅ OK' : '❌ Échec'}</td>
                      <td>{resultat.idDolibarr ?? '—'}</td>
                      <td>{resultat.message ?? '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </section>
  );
}
