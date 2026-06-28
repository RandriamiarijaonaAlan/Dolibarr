import { useEffect, useState } from 'react';
import ActionButton from '../components/ActionButton.jsx';
import CarteStatistique from '../components/CarteStatistique.jsx';
import TableauStatistique from '../components/TableauStatistique.jsx';
import { recupererDashboard } from '../services/dashboardService.js';

export default function PageStatistiques() {
  const [dashboard, setDashboard] = useState(null);
  const [chargement, setChargement] = useState(false);
  const [erreur, setErreur] = useState('');

  useEffect(() => {
    chargerDashboard();
  }, []);

  async function chargerDashboard() {
    setChargement(true);
    setErreur('');
    try {
      const resultat = await recupererDashboard();
      setDashboard(resultat);
    } catch (err) {
      setErreur(err.message || 'API dashboard indisponible');
    } finally {
      setChargement(false);
    }
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
    <div className="page-inner">
      <div className="page-inner-header">
        <h1>Statistiques</h1>
        <p>Salaires agrégés depuis Dolibarr</p>
      </div>

      <CarteStatistique titre="Statistiques salaires">
        <div className="stat-actions">
          <ActionButton variant="secondary" disabled={chargement} onClick={chargerDashboard}>
            {chargement ? 'Chargement...' : 'Actualiser'}
          </ActionButton>
        </div>

        {erreur && <p className="message message--error">{erreur}</p>}
        {chargement && <p className="empty-message">Chargement des statistiques...</p>}

        {!chargement && !erreur && dashboard && (
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
    </div>
  );
}
