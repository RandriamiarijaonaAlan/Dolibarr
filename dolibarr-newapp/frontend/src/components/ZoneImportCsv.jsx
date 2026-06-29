import { useRef, useState } from 'react';
import ActionButton from './ActionButton.jsx';

/**
 * Zone de dépôt + prévisualisation d'un fichier CSV.
 * Le parsing/validation est fourni par le parent via `onAnalyser` ; le résultat est
 * remonté via `onAnalyseChange`. L'import lui-même est déclenché par le parent (bouton
 * global unique) : cette zone n'a pas de bouton d'import, mais affiche le `rapport` reçu.
 *
 * Props :
 *  - titre, description : identification de la zone
 *  - colonnesApercu : [{ cle, libelle, formater? }] colonnes du tableau de prévisualisation
 *  - onAnalyser(fichier) -> { valides, erreurs, colonnesManquantes }
 *  - onAnalyseChange(resultat|null) : remonte l'analyse au parent (null = réinitialisée)
 *  - rapport : rapport d'import renvoyé par le backend (affiché s'il est présent)
 *  - desactive : verrou de l'interface pendant un import en cours
 */
export default function ZoneImportCsv({
  titre,
  description,
  colonnesApercu,
  onAnalyser,
  onAnalyseChange,
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

    reinitialiser();
    setNomFichier(fichier.name);

    try {
      const resultat = await onAnalyser(fichier);

      if (resultat.colonnesManquantes?.length > 0) {
        setErreurAnalyse(`Colonnes manquantes : ${resultat.colonnesManquantes.join(', ')}`);
        return;
      }

      setAnalyse(resultat);
      onAnalyseChange?.(resultat);
    } catch (err) {
      setErreurAnalyse(err.message || 'Lecture du fichier impossible');
    }
  }

  function gererDepot(event) {
    event.preventDefault();
    setSurvol(false);
    if (desactive) return;
    traiterFichier(event.dataTransfer.files?.[0]);
  }

  const aDesLignesValides = analyse?.valides?.length > 0;

  return (
    <section className="import-zone">
      <div className="import-zone-entete">
        <h2>{titre}</h2>
        <p>{description}</p>
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
          accept=".csv,text/csv"
          className="import-depot-input"
          disabled={desactive}
          onChange={(event) => traiterFichier(event.target.files?.[0])}
        />
        <span className="import-depot-icone">📄</span>
        <span>{nomFichier || 'Glisser un fichier CSV ici ou cliquer pour parcourir'}</span>
      </div>

      {erreurAnalyse && <p className="message message--error">{erreurAnalyse}</p>}

      {analyse && (
        <div className="import-apercu">
          <div className="import-apercu-resume">
            <span className="import-badge import-badge--ok">{analyse.valides.length} ligne(s) valide(s)</span>
            <span className="import-badge import-badge--erreur">{analyse.erreurs.length} en erreur</span>
            <button className="import-lien-annuler" disabled={desactive} onClick={reinitialiser}>
              Annuler
            </button>
          </div>

          {aDesLignesValides && (
            <div className="table-wrapper">
              <table className="stat-table">
                <thead>
                  <tr>
                    <th>Ligne</th>
                    {colonnesApercu.map((colonne) => (
                      <th key={colonne.cle}>{colonne.libelle}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {analyse.valides.map((ligne) => (
                    <tr key={ligne.ligne}>
                      <td>{ligne.ligne}</td>
                      {colonnesApercu.map((colonne) => (
                        <td key={colonne.cle}>
                          {colonne.formater ? colonne.formater(ligne[colonne.cle], ligne) : ligne[colonne.cle]}
                        </td>
                      ))}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {analyse.erreurs.length > 0 && (
            <div className="import-erreurs">
              <h3>Lignes en erreur (non importées)</h3>
              <ul>
                {analyse.erreurs.map((erreur) => (
                  <li key={erreur.ligne}>
                    <strong>Ligne {erreur.ligne}</strong> : {erreur.raisons.join(', ')}
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
                    <th>Référence</th>
                    <th>Statut</th>
                    <th>ID Dolibarr</th>
                    <th>Détail</th>
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
