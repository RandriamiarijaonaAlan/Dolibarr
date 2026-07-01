import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ActionButton from '../components/ActionButton.jsx';
import { recupererSalariesFrontoffice } from '../services/salarieService.js';

export default function ListeSalariesPage() {
  const navigate = useNavigate();
  const [salaries, setSalaries] = useState([]);
  const [chargement, setChargement] = useState(false);
  const [erreur, setErreur] = useState('');

  useEffect(() => {
    chargerSalaries();
  }, []);

  async function chargerSalaries() {
    setChargement(true);
    setErreur('');

    try {
      setSalaries(await recupererSalariesFrontoffice());
    } catch (err) {
      setErreur(err.message || 'API salaries indisponible');
    } finally {
      setChargement(false);
    }
  }

  return (
    <div className="fo-page">
      <div className="fo-page-entete">
        <div>
          <h1>Liste salaries</h1>
          <p>{salaries.length} salarie(s)</p>
        </div>
        <ActionButton variant="secondary" disabled={chargement} onClick={chargerSalaries}>
          {chargement ? 'Chargement...' : 'Actualiser'}
        </ActionButton>
      </div>

      {erreur && <p className="message message--error">{erreur}</p>}
      {chargement && <p className="empty-message">Chargement...</p>}

      {!chargement && !erreur && (
        <div className="table-wrapper">
          <table className="stat-table fo-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Reference employe</th>
                <th>Nom</th>
                <th>Identifiant</th>
                <th>Poste</th>
                <th>Genre</th>
                <th>Heures de travail</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {salaries.map((salarie) => (
                <tr key={salarie.id}>
                  <td>{salarie.id}</td>
                  <td>{salarie.refEmploye || '-'}</td>
                  <td>{salarie.nom || '-'}</td>
                  <td>{salarie.identifiant || '-'}</td>
                  <td>{salarie.poste || '-'}</td>
                  <td>{salarie.genre || '-'}</td>
                  <td>{salarie.heureTravailSemaine ?? '-'}</td>
                  <td>
                    <button className="fo-lien" type="button" onClick={() => navigate(`/frontoffice/salaries/${salarie.id}`)}>
                      Voir detail
                    </button>
                  </td>
                </tr>
              ))}
              {salaries.length === 0 && (
                <tr>
                  <td colSpan="8" className="empty-message">Aucun salarie disponible.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
