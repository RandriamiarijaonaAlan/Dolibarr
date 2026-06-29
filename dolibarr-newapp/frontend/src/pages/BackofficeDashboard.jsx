import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ActionButton from '../components/ActionButton.jsx';
import CarteStatistique from '../components/CarteStatistique.jsx';
import TableauStatistique from '../components/TableauStatistique.jsx';
import { oublierCodeBackoffice, reinitialiserDonnees } from '../services/backofficeService.js';
import { recupererDashboard } from '../services/dashboardService.js';

export default function BackofficeDashboard() {
  const navigate = useNavigate();
  const [message, setMessage] = useState('');
  const [details, setDetails] = useState(null);
  const [chargement, setChargement] = useState(false);
  const [dashboard, setDashboard] = useState(null);
  const [chargementDashboard, setChargementDashboard] = useState(false);
  const [erreurDashboard, setErreurDashboard] = useState('');

  useEffect(() => {
    chargerDashboard();
  }, []);

  async function chargerDashboard() {
    setChargementDashboard(true);
    setErreurDashboard('');

    try {
      const resultat = await recupererDashboard();
      setDashboard(resultat);
    } catch (erreur) {
      setErreurDashboard(erreur.message || 'API dashboard indisponible');
    } finally {
      setChargementDashboard(false);
    }
  }

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
      await chargerDashboard();
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

  function formaterMontant(montant) {
    return new Intl.NumberFormat('fr-FR', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(Number(montant || 0));
  }

  const colonnesGenre = [
    { cle: 'genre', libelle: 'Genre' },
    { cle: 'montantTotal', libelle: 'Montant total', formater: formaterMontant },
  ];

  const colonnesMois = [
    { cle: 'mois', libelle: 'Mois' },
    { cle: 'montantTotal', libelle: 'Montant total', formater: formaterMontant },
  ];

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

      <CarteStatistique titre="Statistiques salaires">
        <div className="stat-actions">
          <ActionButton variant="secondary" disabled={chargementDashboard} onClick={chargerDashboard}>
            {chargementDashboard ? 'Chargement...' : 'Actualiser'}
          </ActionButton>
        </div>

        {erreurDashboard && <p className="message message--error">{erreurDashboard}</p>}
        {chargementDashboard && <p className="empty-message">Chargement des statistiques...</p>}

        {!chargementDashboard && !erreurDashboard && dashboard && (
          <div className="stat-grid">
            <TableauStatistique
              titre="Montant de salaire par genre"
              colonnes={colonnesGenre}
              lignes={dashboard.montantParGenre || []}
            />
            <TableauStatistique
              titre="Montant de salaire par mois"
              colonnes={colonnesMois}
              lignes={dashboard.montantParMois || []}
            />
          </div>
        )}
      </CarteStatistique>

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
