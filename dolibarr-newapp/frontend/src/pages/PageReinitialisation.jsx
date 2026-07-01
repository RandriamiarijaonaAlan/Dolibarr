import { useState } from 'react';
import ActionButton from '../components/ActionButton.jsx';
import { reinitialiserDonnees } from '../services/backofficeService.js';

export default function PageReinitialisation() {
  const [chargement, setChargement] = useState(false);
  const [message, setMessage] = useState('');
  const [messageErreur, setMessageErreur] = useState(false);
  const [details, setDetails] = useState(null);

  async function lancerReinitialisation() {
    const confirmation = window.confirm('Confirmer la reinitialisation des donnees Dolibarr et SQLite ?');
    if (!confirmation) return;

    setChargement(true);
    setMessage('');
    setMessageErreur(false);
    setDetails(null);

    try {
      const resultat = await reinitialiserDonnees();
      setMessage(resultat.message);
      setMessageErreur(false);
      setDetails(resultat);
    } catch (err) {
      setMessage(err.message);
      setMessageErreur(true);
    } finally {
      setChargement(false);
    }
  }

  return (
    <div className="page-inner">
      <div className="page-inner-header">
        <h1>Reinitialisation</h1>
        <p>Suppression des donnees Dolibarr et SQLite autorisees</p>
      </div>

      <div className="reinit-card">
        <p>
          Cette action supprime les donnees autorisees dans Dolibarr et vide aussi les jours
          feries stockes en SQLite. Les utilisateurs proteges et les donnees systeme sont
          preserves.
        </p>

        <ActionButton variant="danger" disabled={chargement} onClick={lancerReinitialisation}>
          {chargement ? 'Reinitialisation en cours...' : 'Reinitialiser les donnees'}
        </ActionButton>

        {message && (
          <p className={'message' + (messageErreur ? ' message--error' : '')}>{message}</p>
        )}

        {details && (
          <div className="reinit-resultats">
            <h3>Resultats</h3>
            <ul className="reinit-liste">
              <li>
                <span>Users supprimes</span>
                <strong>{details.usersDeleted}</strong>
              </li>
              <li>
                <span>Users proteges ignores</span>
                <strong>{details.usersSkippedProtected}</strong>
              </li>
              <li>
                <span>Users non NewApp ignores</span>
                <strong>{details.usersSkippedNotNewApp}</strong>
              </li>
              <li>
                <span>Salaires supprimes</span>
                <strong>{details.salariesDeleted}</strong>
              </li>
              <li>
                <span>Paiements supprimes</span>
                <strong>{details.paymentsDeleted}</strong>
              </li>
              <li>
                <span>Jours feries SQLite supprimes</span>
                <strong>{details.joursFeriesDeleted}</strong>
              </li>
            </ul>
            {details.errors?.length > 0 && (
              <div className="reinit-erreurs">
                {details.errors.map((erreur) => (
                  <p key={erreur} className="message message--error">
                    {erreur}
                  </p>
                ))}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
