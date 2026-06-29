import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import ActionButton from '../components/ActionButton.jsx';
import { creerSalaire, recupererEmployes } from '../services/frontofficeService.js';

export default function PageCreerSalaire() {
  const navigate = useNavigate();
  const [parametres] = useSearchParams();

  const [employes, setEmployes] = useState([]);
  const [fkUser, setFkUser] = useState(parametres.get('employe') || '');
  const [montant, setMontant] = useState('');
  const [dateDebut, setDateDebut] = useState('');
  const [dateFin, setDateFin] = useState('');

  const [erreur, setErreur] = useState('');
  const [envoi, setEnvoi] = useState(false);
  const [resultat, setResultat] = useState(null); // { id } après création réussie

  useEffect(() => {
    recupererEmployes().then(setEmployes).catch(() => setEmployes([]));
  }, []);

  function valider() {
    if (!fkUser) return 'Sélectionnez un employé';
    const montantNombre = Number(montant);
    if (!montant || Number.isNaN(montantNombre) || montantNombre <= 0) return 'Le montant doit être supérieur à 0';
    if (!dateDebut || !dateFin) return 'Les dates de début et de fin sont obligatoires';
    if (dateFin < dateDebut) return 'La date de fin doit être postérieure ou égale à la date de début';
    return '';
  }

  async function soumettre(event) {
    event.preventDefault();
    const messageErreur = valider();
    if (messageErreur) {
      setErreur(messageErreur);
      return;
    }

    setEnvoi(true);
    setErreur('');
    try {
      const reponse = await creerSalaire({
        fkUser: Number(fkUser),
        montant: Number(montant),
        dateDebut,
        dateFin,
      });
      if (reponse.success) {
        setResultat(reponse);
      } else {
        setErreur(reponse.message);
      }
    } catch (err) {
      setErreur(err.message || 'Échec de la création');
    } finally {
      setEnvoi(false);
    }
  }

  if (resultat) {
    return (
      <div className="fo-page fo-page--etroite">
        <h1>Salaire créé</h1>
        <p className="message">Le salaire <strong>#{resultat.id}</strong> a été créé avec succès.</p>
        <div className="fo-actions">
          <ActionButton onClick={() => navigate(`/salaries/${resultat.id}/payer`)}>
            Effectuer un premier paiement
          </ActionButton>
          <ActionButton variant="secondary" onClick={() => navigate('/salaries')}>
            Retour à la liste
          </ActionButton>
        </div>
      </div>
    );
  }

  return (
    <div className="fo-page fo-page--etroite">
      <button className="fo-lien" onClick={() => navigate(-1)}>← Retour</button>
      <h1>Créer un salaire</h1>

      <form className="form fo-form" onSubmit={soumettre}>
        <label>
          Employé
          <select value={fkUser} onChange={(e) => setFkUser(e.target.value)}>
            <option value="">— Sélectionner —</option>
            {employes.map((employe) => (
              <option key={employe.id} value={employe.id}>
                {employe.nom} ({employe.login})
              </option>
            ))}
          </select>
        </label>

        <label>
          Montant
          <input
            type="number"
            step="0.01"
            min="0"
            value={montant}
            placeholder="Ex : 500.00 (séparateur point)"
            onChange={(e) => setMontant(e.target.value)}
          />
        </label>

        <label>
          Date de début
          <input type="date" value={dateDebut} onChange={(e) => setDateDebut(e.target.value)} />
        </label>

        <label>
          Date de fin
          <input type="date" value={dateFin} onChange={(e) => setDateFin(e.target.value)} />
        </label>

        {erreur && <p className="message message--error">{erreur}</p>}

        <ActionButton type="submit" disabled={envoi}>
          {envoi ? 'Création...' : 'Créer le salaire'}
        </ActionButton>
      </form>
    </div>
  );
}
