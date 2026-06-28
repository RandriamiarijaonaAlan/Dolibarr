import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ActionButton from '../components/ActionButton.jsx';
import { oublierCodeBackoffice, reinitialiserDonnees } from '../services/backofficeService.js';

export default function BackofficeDashboard() {
  const navigate = useNavigate();
  const [message, setMessage] = useState('');
  const [details, setDetails] = useState(null);
  const [chargement, setChargement] = useState(false);

  async function lancerReinitialisation() {
    const confirmation = window.confirm('Confirmer la réinitialisation des données Dolibarr ?');

    if (!confirmation) {
      return;
    }

    setChargement(true);
    setMessage('');
    setDetails(null);

    try {
      const resultat = await reinitialiserDonnees();
      setMessage(resultat.message);
      setDetails(resultat);
    } catch (erreur) {
      setMessage(erreur.message);
    } finally {
      setChargement(false);
    }
  }

  function quitterBackoffice() {
    oublierCodeBackoffice();
    navigate('/backoffice/access');
  }

  return (
    <main className="page">
      <header className="dashboard-header">
        <div>
          <h1>Dashboard Backoffice</h1>
          <p>Administration des données Dolibarr</p>
        </div>
        <ActionButton variant="secondary" onClick={quitterBackoffice}>
          Quitter
        </ActionButton>
      </header>

      <section className="dashboard-section">
        <h2>Réinitialisation</h2>
        <p>Supprime depuis Dolibarr uniquement les données autorisées via le backend Spring Boot.</p>
        <ActionButton variant="danger" disabled={chargement} onClick={lancerReinitialisation}>
          {chargement ? 'Réinitialisation...' : 'Réinitialiser les données'}
        </ActionButton>
      </section>

      {message && <p className="message">{message}</p>}

      {details && (
        <section className="resultats">
          <p>Users supprimés : {details.usersDeleted}</p>
          <p>Users protégés ignorés : {details.usersSkippedProtected}</p>
          <p>Users non NewApp ignorés : {details.usersSkippedNotNewApp}</p>
          <p>Salaires supprimés : {details.salariesDeleted}</p>
          <p>Paiements supprimés : {details.paymentsDeleted}</p>
          {details.errors?.map((erreur) => (
            <p key={erreur}>{erreur}</p>
          ))}
        </section>
      )}
    </main>
  );
}
