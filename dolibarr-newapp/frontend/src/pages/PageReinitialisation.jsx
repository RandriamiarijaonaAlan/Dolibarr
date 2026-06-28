import { useState } from 'react';
import ActionButton from '../components/ActionButton.jsx';
import { reinitialiserDonnees } from '../services/backofficeService.js';

export default function PageReinitialisation() {
  const [chargement, setChargement] = useState(false);
  const [message, setMessage] = useState('');
  const [messageErreur, setMessageErreur] = useState(false);
  const [details, setDetails] = useState(null);

  async function lancerReinitialisation() {
    const confirmation = window.confirm('Confirmer la réinitialisation des données Dolibarr ?');
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
        <h1>Réinitialisation</h1>
        <p>Suppression des données Dolibarr autorisées</p>
      </div>

      <div className="reinit-card">
        <p>
          Cette action supprime depuis Dolibarr uniquement les données autorisées via le backend
          Spring Boot. Les utilisateurs protégés et les données système sont préservés.
        </p>

        <ActionButton variant="danger" disabled={chargement} onClick={lancerReinitialisation}>
          {chargement ? 'Réinitialisation en cours...' : 'Réinitialiser les données'}
        </ActionButton>

        {message && (
          <p className={'message' + (messageErreur ? ' message--error' : '')}>{message}</p>
        )}

        {details && (
          <div className="reinit-resultats">
            <h3>Résultats</h3>
            <ul className="reinit-liste">
              <li>
                <span>Users supprimés</span>
                <strong>{details.usersDeleted}</strong>
              </li>
              <li>
                <span>Users protégés ignorés</span>
                <strong>{details.usersSkippedProtected}</strong>
              </li>
              <li>
                <span>Users non NewApp ignorés</span>
                <strong>{details.usersSkippedNotNewApp}</strong>
              </li>
              <li>
                <span>Salaires supprimés</span>
                <strong>{details.salariesDeleted}</strong>
              </li>
              <li>
                <span>Paiements supprimés</span>
                <strong>{details.paymentsDeleted}</strong>
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
