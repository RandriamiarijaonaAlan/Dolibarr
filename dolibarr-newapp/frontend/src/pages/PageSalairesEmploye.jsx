import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import ActionButton from '../components/ActionButton.jsx';
import { recupererSalairesEmploye } from '../services/frontofficeService.js';
import { descriptionStatut, formaterDate, formaterMontant } from '../utils/formats.js';

export default function PageSalairesEmploye() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [salaires, setSalaires] = useState([]);
  const [chargement, setChargement] = useState(false);
  const [erreur, setErreur] = useState('');

  useEffect(() => {
    charger();
  }, [id]);

  async function charger() {
    setChargement(true);
    setErreur('');
    try {
      setSalaires(await recupererSalairesEmploye(id));
    } catch (err) {
      setErreur(err.message || 'Impossible de charger les salaires');
    } finally {
      setChargement(false);
    }
  }

  const employe = salaires[0];

  return (
    <div className="fo-page">
      <button className="fo-lien" onClick={() => navigate('/salaries')}>← Retour aux salariés</button>

      <div className="fo-page-entete">
        <div>
          <h1>Salaires de {employe?.nomEmploye || `l'employé #${id}`}</h1>
          <p>{salaires.length} salaire(s)</p>
        </div>
        <ActionButton onClick={() => navigate(`/salaries/creer?employe=${id}`)}>Créer un salaire</ActionButton>
      </div>

      {erreur && <p className="message message--error">{erreur}</p>}
      {chargement && <p className="empty-message">Chargement...</p>}

      {!chargement && !erreur && (
        <div className="table-wrapper">
          <table className="stat-table fo-table">
            <thead>
              <tr>
                <th>#</th>
                <th>Période</th>
                <th>Montant</th>
                <th>Déjà payé</th>
                <th>Reste</th>
                <th>Statut</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {salaires.map((salaire) => {
                const statut = descriptionStatut(salaire.statut);
                return (
                  <tr key={salaire.id}>
                    <td>{salaire.id}</td>
                    <td>{formaterDate(salaire.dateDebut)} → {formaterDate(salaire.dateFin)}</td>
                    <td>{formaterMontant(salaire.montant)}</td>
                    <td>{formaterMontant(salaire.totalPaye)}</td>
                    <td>{formaterMontant(salaire.resteAPayer)}</td>
                    <td><span className={`fo-statut ${statut.classe}`}>{statut.icone} {statut.libelle}</span></td>
                    <td>
                      <button className="fo-lien" onClick={() => navigate(`/salaries/${salaire.id}/payer`)}>
                        Payer →
                      </button>
                    </td>
                  </tr>
                );
              })}
              {salaires.length === 0 && (
                <tr>
                  <td colSpan="7" className="empty-message">Aucun salaire pour cet employé.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
