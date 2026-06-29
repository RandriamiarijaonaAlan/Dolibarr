import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ActionButton from '../components/ActionButton.jsx';
import PhotoEmploye from '../components/PhotoEmploye.jsx';
import { recupererEmployes } from '../services/frontofficeService.js';
import { formaterMontant, libelleGenre } from '../utils/formats.js';

export default function PageSalariesListe() {
  const navigate = useNavigate();
  const [employes, setEmployes] = useState([]);
  const [chargement, setChargement] = useState(false);
  const [erreur, setErreur] = useState('');

  // Critères de recherche (filtrage côté client, combinés en ET logique).
  const [nom, setNom] = useState('');
  const [genre, setGenre] = useState('tous');
  const [heuresMin, setHeuresMin] = useState('');
  const [heuresMax, setHeuresMax] = useState('');

  useEffect(() => {
    chargerEmployes();
  }, []);

  async function chargerEmployes() {
    setChargement(true);
    setErreur('');
    try {
      setEmployes(await recupererEmployes());
    } catch (err) {
      setErreur(err.message || 'Impossible de charger les salariés');
    } finally {
      setChargement(false);
    }
  }

  const employesFiltres = useMemo(() => {
    const recherche = nom.trim().toLowerCase();
    const min = heuresMin === '' ? null : Number(heuresMin);
    const max = heuresMax === '' ? null : Number(heuresMax);

    return employes.filter((employe) => {
      if (recherche && !(employe.nom || '').toLowerCase().includes(recherche)) return false;
      if (genre !== 'tous' && employe.genre !== genre) return false;
      const heures = employe.heuresSemaine ?? 0;
      if (min !== null && heures < min) return false;
      if (max !== null && heures > max) return false;
      return true;
    });
  }, [employes, nom, genre, heuresMin, heuresMax]);

  function reinitialiserFiltres() {
    setNom('');
    setGenre('tous');
    setHeuresMin('');
    setHeuresMax('');
  }

  return (
    <div className="fo-page">
      <div className="fo-page-entete">
        <div>
          <h1>Salariés</h1>
          <p>{employesFiltres.length} salarié(s) affiché(s) sur {employes.length}</p>
        </div>
        <ActionButton onClick={() => navigate('/salaries/creer')}>Créer un salaire</ActionButton>
      </div>

      <div className="fo-filtres">
        <label>
          Nom
          <input type="text" value={nom} placeholder="Recherche partielle" onChange={(e) => setNom(e.target.value)} />
        </label>
        <label>
          Genre
          <select value={genre} onChange={(e) => setGenre(e.target.value)}>
            <option value="tous">Tous</option>
            <option value="man">Homme</option>
            <option value="woman">Femme</option>
          </select>
        </label>
        <label>
          Heures min
          <input type="number" min="0" value={heuresMin} onChange={(e) => setHeuresMin(e.target.value)} />
        </label>
        <label>
          Heures max
          <input type="number" min="0" value={heuresMax} onChange={(e) => setHeuresMax(e.target.value)} />
        </label>
        <button className="fo-lien" onClick={reinitialiserFiltres}>Réinitialiser</button>
      </div>

      {erreur && <p className="message message--error">{erreur}</p>}
      {chargement && <p className="empty-message">Chargement...</p>}

      {!chargement && !erreur && (
        <div className="table-wrapper">
          <table className="stat-table fo-table">
            <thead>
              <tr>
                <th>Photo</th>
                <th>Nom</th>
                <th>Identifiant</th>
                <th>Genre</th>
                <th>Heures/sem.</th>
                <th>Salaires</th>
                <th>Montant total</th>
                <th>Reste à payer</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {employesFiltres.map((employe) => (
                <tr
                  key={employe.id}
                  className="fo-ligne-cliquable"
                  onClick={() => navigate(`/salaries/employe/${employe.id}`)}
                >
                  <td><PhotoEmploye id={employe.id} nom={employe.nom} taille="petite" /></td>
                  <td>{employe.nom || '—'}</td>
                  <td>{employe.login || '—'}</td>
                  <td>{libelleGenre(employe.genre)}</td>
                  <td>{employe.heuresSemaine ?? '—'}</td>
                  <td>{employe.nbSalaires}</td>
                  <td>{formaterMontant(employe.montantTotal)}</td>
                  <td>{formaterMontant(employe.resteAPayer)}</td>
                  <td><span className="fo-lien">Voir ses salaires →</span></td>
                </tr>
              ))}
              {employesFiltres.length === 0 && (
                <tr>
                  <td colSpan="9" className="empty-message">Aucun salarié ne correspond aux critères.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
