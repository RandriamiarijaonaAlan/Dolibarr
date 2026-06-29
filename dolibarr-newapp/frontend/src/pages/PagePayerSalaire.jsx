import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import ActionButton from '../components/ActionButton.jsx';
import PhotoEmploye from '../components/PhotoEmploye.jsx';
import { ajouterPaiement, recupererModesPaiement, recupererSalaire } from '../services/frontofficeService.js';
import { descriptionStatut, formaterDate, formaterMontant, libelleGenre } from '../utils/formats.js';

function aujourdhuiIso() {
  return new Date().toISOString().slice(0, 10);
}

export default function PagePayerSalaire() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [salaire, setSalaire] = useState(null);
  const [modes, setModes] = useState([]);
  const [chargement, setChargement] = useState(false);
  const [erreur, setErreur] = useState('');

  // Formulaire de paiement.
  const [montant, setMontant] = useState('');
  const [date, setDate] = useState(aujourdhuiIso());
  const [mode, setMode] = useState('');
  const [envoi, setEnvoi] = useState(false);
  const [messageSucces, setMessageSucces] = useState('');
  const [erreurForm, setErreurForm] = useState('');

  useEffect(() => {
    recupererModesPaiement().then(setModes).catch(() => setModes([]));
  }, []);

  useEffect(() => {
    charger();
  }, [id]);

  // Relit toujours depuis Dolibarr (pas de cache) et pré-remplit le montant avec le reste à payer.
  async function charger() {
    setChargement(true);
    setErreur('');
    try {
      const detail = await recupererSalaire(id);
      setSalaire(detail);
      setMontant(detail && detail.resteAPayer > 0 ? String(detail.resteAPayer) : '');
    } catch (err) {
      setErreur(err.message || 'Salaire introuvable');
    } finally {
      setChargement(false);
    }
  }

  async function soumettre(event) {
    event.preventDefault();
    setErreurForm('');
    setMessageSucces('');

    const montantNombre = Number(montant);
    if (!montant || Number.isNaN(montantNombre) || montantNombre <= 0) {
      setErreurForm('Le montant doit être supérieur à 0');
      return;
    }
    if (montantNombre > salaire.resteAPayer) {
      setErreurForm(`Le montant ne peut pas dépasser le reste à payer (${formaterMontant(salaire.resteAPayer)})`);
      return;
    }

    setEnvoi(true);
    try {
      const reponse = await ajouterPaiement(id, {
        montant: montantNombre,
        date,
        fkTypePayment: mode === '' ? null : Number(mode),
      });
      if (reponse.success) {
        setMessageSucces('Paiement enregistré.');
        await charger(); // rafraîchit historique + reste à payer
      } else {
        setErreurForm(reponse.message);
      }
    } catch (err) {
      setErreurForm(err.message || 'Échec du paiement');
    } finally {
      setEnvoi(false);
    }
  }

  if (chargement && !salaire) return <div className="fo-page"><p className="empty-message">Chargement...</p></div>;
  if (erreur) return <div className="fo-page"><p className="message message--error">{erreur}</p></div>;
  if (!salaire) return null;

  const statut = descriptionStatut(salaire.statut);
  const solde = salaire.resteAPayer <= 0;

  return (
    <div className="fo-page fo-page--etroite">
      <button className="fo-lien" onClick={() => navigate(-1)}>← Retour</button>

      <div className="fo-fiche">
        <div className="fo-fiche-entete">
          <PhotoEmploye id={salaire.idEmploye} nom={salaire.nomEmploye} taille="grande" />
          <div>
            <h1>Salaire #{salaire.id} — {salaire.nomEmploye || '—'}</h1>
            <p className="fo-fiche-sous">
              {salaire.loginEmploye} · {libelleGenre(salaire.genreEmploye)}
            </p>
            <p className="fo-fiche-sous">
              Période : {formaterDate(salaire.dateDebut)} → {formaterDate(salaire.dateFin)}
            </p>
          </div>
          <div className="fo-fiche-montant">
            <span>Montant total</span>
            <strong>{formaterMontant(salaire.montant)}</strong>
          </div>
        </div>

        <h2>Historique des paiements</h2>
        {salaire.paiements?.length > 0 ? (
          <table className="stat-table">
            <thead>
              <tr><th>Date</th><th>Montant</th><th>Mode</th></tr>
            </thead>
            <tbody>
              {salaire.paiements.map((paiement, index) => (
                <tr key={index}>
                  <td>{formaterDate(paiement.date)}</td>
                  <td>{formaterMontant(paiement.montant)}</td>
                  <td>{paiement.mode || '—'} ✓</td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : (
          <p className="empty-message">Aucun paiement effectué.</p>
        )}

        <div className="fo-reste">
          <div>
            <span>Reste à payer</span>
            <strong>{formaterMontant(salaire.resteAPayer)}</strong>
          </div>
          <span className={`fo-statut ${statut.classe}`}>{statut.icone} {statut.libelle}</span>
        </div>

        {messageSucces && <p className="message">{messageSucces}</p>}

        {solde ? (
          <p className="message fo-solde">✅ Salaire soldé</p>
        ) : (
          <form className="fo-form-paiement" onSubmit={soumettre}>
            <h3>Nouveau paiement</h3>
            <label>
              Montant (max : {formaterMontant(salaire.resteAPayer)})
              <input
                type="number"
                step="0.01"
                min="0"
                max={salaire.resteAPayer}
                value={montant}
                onChange={(e) => setMontant(e.target.value)}
              />
            </label>
            <label>
              Date
              <input type="date" value={date} onChange={(e) => setDate(e.target.value)} />
            </label>
            <label>
              Mode de paiement
              <select value={mode} onChange={(e) => setMode(e.target.value)}>
                <option value="">— Choisir —</option>
                {modes.map((m) => (
                  <option key={m.id} value={m.id}>{m.libelle}</option>
                ))}
              </select>
            </label>

            {erreurForm && <p className="message message--error">{erreurForm}</p>}

            <ActionButton type="submit" disabled={envoi}>
              {envoi ? 'Paiement...' : 'Payer'}
            </ActionButton>
          </form>
        )}
      </div>
    </div>
  );
}
