import { useEffect, useState } from 'react';
import ActionButton from '../components/ActionButton.jsx';
import {
  creerJourFerie,
  effacerJourFerie,
  listerJoursFeries,
  mettreAJourJourFerie,
} from '../services/jourFerieService.js';

const FORMULAIRE_INITIAL = {
  nom: '',
  dateJour: '',
  description: '',
  actif: true,
};

export default function JoursFeriesPage() {
  const [joursFeries, setJoursFeries] = useState([]);
  const [formulaire, setFormulaire] = useState(FORMULAIRE_INITIAL);
  const [idSelectionne, setIdSelectionne] = useState(null);
  const [chargement, setChargement] = useState(false);
  const [enregistrement, setEnregistrement] = useState(false);
  const [message, setMessage] = useState('');
  const [erreur, setErreur] = useState('');

  useEffect(() => {
    chargerJoursFeries();
  }, []);

  async function chargerJoursFeries() {
    setChargement(true);
    setErreur('');

    try {
      setJoursFeries(await listerJoursFeries());
    } catch (err) {
      setErreur(err.message || 'Impossible de charger les jours feries');
    } finally {
      setChargement(false);
    }
  }

  async function ajouterJourFerie() {
    setEnregistrement(true);
    setErreur('');
    setMessage('');

    try {
      await creerJourFerie(formulaire);
      setMessage('Jour ferie ajoute');
      reinitialiserFormulaire();
      await chargerJoursFeries();
    } catch (err) {
      setErreur(err.message || "Impossible d'ajouter le jour ferie");
    } finally {
      setEnregistrement(false);
    }
  }

  async function modifierJourFerie() {
    if (!idSelectionne) {
      return;
    }

    setEnregistrement(true);
    setErreur('');
    setMessage('');

    try {
      await mettreAJourJourFerie(idSelectionne, formulaire);
      setMessage('Jour ferie modifie');
      reinitialiserFormulaire();
      await chargerJoursFeries();
    } catch (err) {
      setErreur(err.message || 'Impossible de modifier le jour ferie');
    } finally {
      setEnregistrement(false);
    }
  }

  async function supprimerJourFerie(jourFerie) {
    if (!window.confirm(`Supprimer le jour ferie "${jourFerie.nom}" ?`)) {
      return;
    }

    setErreur('');
    setMessage('');

    try {
      await effacerJourFerie(jourFerie.id);
      setMessage('Jour ferie supprime');
      if (idSelectionne === jourFerie.id) {
        reinitialiserFormulaire();
      }
      await chargerJoursFeries();
    } catch (err) {
      setErreur(err.message || 'Impossible de supprimer le jour ferie');
    }
  }

  function reinitialiserFormulaire() {
    setFormulaire(FORMULAIRE_INITIAL);
    setIdSelectionne(null);
  }

  function selectionnerJourFerie(jourFerie) {
    setIdSelectionne(jourFerie.id);
    setFormulaire({
      nom: jourFerie.nom || '',
      dateJour: jourFerie.dateJour || '',
      description: jourFerie.description || '',
      actif: Boolean(jourFerie.actif),
    });
    setErreur('');
    setMessage('');
  }

  function modifierChamp(champ, valeur) {
    setFormulaire((courant) => ({
      ...courant,
      [champ]: valeur,
    }));
  }

  async function soumettreFormulaire(event) {
    event.preventDefault();
    if (idSelectionne) {
      await modifierJourFerie();
    } else {
      await ajouterJourFerie();
    }
  }

  return (
    <div className="page-inner">
      <div className="page-inner-header">
        <h1>Jours feries</h1>
        <p>Gestion locale SQLite, sans appel direct a Dolibarr depuis React</p>
      </div>

      <section className="jours-feries-section">
        <form className="jours-feries-form" onSubmit={soumettreFormulaire}>
          <h2>{idSelectionne ? 'Modifier' : 'Ajouter'}</h2>

          <label>
            Nom
            <input
              type="text"
              value={formulaire.nom}
              onChange={(event) => modifierChamp('nom', event.target.value)}
              required
            />
          </label>

          <label>
            Date
            <input
              type="date"
              value={formulaire.dateJour}
              onChange={(event) => modifierChamp('dateJour', event.target.value)}
              required
            />
          </label>

          <label>
            Description
            <textarea
              value={formulaire.description}
              onChange={(event) => modifierChamp('description', event.target.value)}
              rows="3"
            />
          </label>

          <label className="jours-feries-check">
            <input
              type="checkbox"
              checked={formulaire.actif}
              onChange={(event) => modifierChamp('actif', event.target.checked)}
            />
            Actif
          </label>

          <div className="fo-actions">
            <ActionButton type="submit" disabled={enregistrement}>
              {enregistrement ? 'Enregistrement...' : idSelectionne ? 'Modifier' : 'Ajouter'}
            </ActionButton>
            {idSelectionne && (
              <ActionButton type="button" variant="secondary" onClick={reinitialiserFormulaire}>
                Annuler
              </ActionButton>
            )}
          </div>
        </form>

        <div className="jours-feries-liste">
          <div className="stat-actions">
            <ActionButton variant="secondary" disabled={chargement} onClick={chargerJoursFeries}>
              {chargement ? 'Chargement...' : 'Actualiser'}
            </ActionButton>
          </div>

          {erreur && <p className="message message--error">{erreur}</p>}
          {message && <p className="message">{message}</p>}

          <div className="table-wrapper">
            <table className="stat-table">
              <thead>
                <tr>
                  <th>Nom</th>
                  <th>Date</th>
                  <th>Description</th>
                  <th>Actif</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {joursFeries.map((jourFerie) => (
                  <tr key={jourFerie.id}>
                    <td>{jourFerie.nom}</td>
                    <td>{jourFerie.dateJour}</td>
                    <td>{jourFerie.description || '-'}</td>
                    <td>{jourFerie.actif ? 'Oui' : 'Non'}</td>
                    <td>
                      <div className="jours-feries-actions">
                        <button className="fo-lien" type="button" onClick={() => selectionnerJourFerie(jourFerie)}>
                          Modifier
                        </button>
                        <button
                          className="fo-lien fo-lien-danger"
                          type="button"
                          onClick={() => supprimerJourFerie(jourFerie)}
                        >
                          Supprimer
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
                {!chargement && joursFeries.length === 0 && (
                  <tr>
                    <td colSpan="5" className="empty-message">
                      Aucun jour ferie.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </section>
    </div>
  );
}
