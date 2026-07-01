import { useEffect, useMemo, useState } from 'react';
import ActionButton from '../components/ActionButton.jsx';
import { rechercherEmployes } from '../services/employeService.js';
import { genererSalaires as envoyerGenerationSalaires } from '../services/salaireService.js';
import { libelleGenre } from '../utils/formats.js';

const FILTRES_INITIAUX = {
  poste: '',
  genre: 'tous',
  heureMin: '',
  heureMax: '',
};

const FORMULAIRE_INITIAL = {
  dateDebut: '',
  dateFin: '',
  montant: '',
};

export default function GenerationSalairesPage() {
  const [filtres, setFiltres] = useState(FILTRES_INITIAUX);
  const [formulaire, setFormulaire] = useState(FORMULAIRE_INITIAL);
  const [employes, setEmployes] = useState([]);
  const [selection, setSelection] = useState([]);
  const [chargement, setChargement] = useState(false);
  const [generation, setGeneration] = useState(false);
  const [erreur, setErreur] = useState('');
  const [message, setMessage] = useState('');
  const [resume, setResume] = useState(null);

  useEffect(() => {
    chargerEmployes();
  }, []);

  const employesSelectionnes = useMemo(
    () => employes.filter((employe) => selection.includes(employe.id)),
    [employes, selection]
  );

  async function chargerEmployes() {
    setChargement(true);
    setErreur('');

    try {
      const resultat = await rechercherEmployes(filtres);
      setEmployes(resultat);
      setSelection((courante) => courante.filter((id) => resultat.some((employe) => employe.id === id)));
    } catch (err) {
      setErreur(err.message || 'Impossible de charger les salaries');
    } finally {
      setChargement(false);
    }
  }

  async function appliquerFiltres(event) {
    event.preventDefault();
    await chargerEmployes();
  }

  function selectionnerEmploye(id) {
    setSelection((courante) => {
      if (courante.includes(id)) {
        return courante.filter((valeur) => valeur !== id);
      }
      return [...courante, id];
    });
  }

  function toutSelectionner() {
    setSelection(employes.map((employe) => employe.id));
  }

  function toutDeselectionner() {
    setSelection([]);
  }

  async function genererSalaires(event) {
    event.preventDefault();
    const erreurValidation = validerFormulaire();
    if (erreurValidation) {
      setErreur(erreurValidation);
      return;
    }

    setGeneration(true);
    setErreur('');
    setMessage('');
    setResume(null);

    try {
      const reponse = await envoyerGenerationSalaires({
        employeIds: selection,
        dateDebut: formulaire.dateDebut,
        dateFin: formulaire.dateFin,
        montant: Number(formulaire.montant),
      });
      setResume(reponse.resume);
      setMessage(reponse.message || 'Generation terminee');
      if (reponse.success) {
        reinitialiserFormulaire();
      }
    } catch (err) {
      setErreur(err.message || 'Impossible de generer les salaires');
    } finally {
      setGeneration(false);
    }
  }

  function reinitialiserFormulaire() {
    setFormulaire(FORMULAIRE_INITIAL);
    setSelection([]);
  }

  function afficherResume() {
    if (!resume) {
      return null;
    }

    return (
      <div className="generation-resume">
        <h2>Resume</h2>
        <ul className="reinit-liste">
          <li><span>Salaires crees</span><strong>{resume.salairesCrees}</strong></li>
          <li><span>Salaires ignores</span><strong>{resume.salairesIgnores}</strong></li>
        </ul>
        {resume.erreurs?.length > 0 && (
          <div className="generation-erreurs">
            {resume.erreurs.map((item) => (
              <p className="message message--error" key={item}>{item}</p>
            ))}
          </div>
        )}
      </div>
    );
  }

  function validerFormulaire() {
    const montant = Number(formulaire.montant);
    if (selection.length === 0) return 'Selectionnez au moins un salarie';
    if (!formulaire.dateDebut) return 'La date de debut est obligatoire';
    if (!formulaire.dateFin) return 'La date de fin est obligatoire';
    if (!formulaire.montant || Number.isNaN(montant) || montant <= 0) return 'Le montant doit etre superieur a 0';
    if (formulaire.dateFin < formulaire.dateDebut) return 'La date de fin doit etre posterieure ou egale a la date de debut';
    return '';
  }

  function modifierFiltre(champ, valeur) {
    setFiltres((courants) => ({ ...courants, [champ]: valeur }));
  }

  function modifierFormulaire(champ, valeur) {
    setFormulaire((courant) => ({ ...courant, [champ]: valeur }));
  }

  return (
    <div className="fo-page">
      <div className="fo-page-entete">
        <div>
          <h1>Generation salaires</h1>
          <p>{employes.length} salarie(s), {selection.length} selectionne(s)</p>
        </div>
      </div>

      <form className="fo-filtres" onSubmit={appliquerFiltres}>
        <label>
          Poste
          <input
            type="text"
            value={filtres.poste}
            placeholder="Commercial"
            onChange={(event) => modifierFiltre('poste', event.target.value)}
          />
        </label>
        <label>
          Genre
          <select value={filtres.genre} onChange={(event) => modifierFiltre('genre', event.target.value)}>
            <option value="tous">Tous</option>
            <option value="man">Homme</option>
            <option value="woman">Femme</option>
          </select>
        </label>
        <label>
          Heures min
          <input type="number" min="0" value={filtres.heureMin} onChange={(event) => modifierFiltre('heureMin', event.target.value)} />
        </label>
        <label>
          Heures max
          <input type="number" min="0" value={filtres.heureMax} onChange={(event) => modifierFiltre('heureMax', event.target.value)} />
        </label>
        <ActionButton type="submit" variant="secondary" disabled={chargement}>
          {chargement ? 'Recherche...' : 'Rechercher'}
        </ActionButton>
      </form>

      {erreur && <p className="message message--error">{erreur}</p>}
      {message && <p className="message">{message}</p>}

      <div className="generation-layout">
        <section className="generation-liste">
          <div className="generation-actions">
            <ActionButton variant="secondary" onClick={toutSelectionner} disabled={employes.length === 0}>
              Tout selectionner
            </ActionButton>
            <ActionButton variant="secondary" onClick={toutDeselectionner} disabled={selection.length === 0}>
              Tout deselectionner
            </ActionButton>
          </div>

          <div className="table-wrapper">
            <table className="stat-table fo-table">
              <thead>
                <tr>
                  <th></th>
                  <th>Reference employe</th>
                  <th>Nom</th>
                  <th>Identifiant</th>
                  <th>Poste</th>
                  <th>Genre</th>
                  <th>Heures/sem.</th>
                </tr>
              </thead>
              <tbody>
                {employes.map((employe) => (
                  <tr key={employe.id}>
                    <td>
                      <input
                        type="checkbox"
                        checked={selection.includes(employe.id)}
                        onChange={() => selectionnerEmploye(employe.id)}
                      />
                    </td>
                    <td>{employe.refEmploye || employe.id}</td>
                    <td>{employe.nom || '-'}</td>
                    <td>{employe.login || '-'}</td>
                    <td>{employe.poste || '-'}</td>
                    <td>{libelleGenre(employe.genre)}</td>
                    <td>{employe.heuresSemaine ?? '-'}</td>
                  </tr>
                ))}
                {!chargement && employes.length === 0 && (
                  <tr>
                    <td colSpan="7" className="empty-message">Aucun salarie ne correspond aux filtres.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>

        <aside className="generation-formulaire">
          <form className="fo-form" onSubmit={genererSalaires}>
            <h2>Formulaire salaire</h2>
            <label>
              Date debut
              <input type="date" value={formulaire.dateDebut} onChange={(event) => modifierFormulaire('dateDebut', event.target.value)} />
            </label>
            <label>
              Date fin
              <input type="date" value={formulaire.dateFin} onChange={(event) => modifierFormulaire('dateFin', event.target.value)} />
            </label>
            <label>
              Montant
              <input
                type="number"
                min="0"
                step="0.01"
                value={formulaire.montant}
                onChange={(event) => modifierFormulaire('montant', event.target.value)}
              />
            </label>
            <p className="empty-message">{employesSelectionnes.length} salarie(s) recevront ce salaire.</p>
            <ActionButton type="submit" disabled={generation}>
              {generation ? 'Generation...' : 'Generer salaire'}
            </ActionButton>
          </form>
          {afficherResume()}
        </aside>
      </div>
    </div>
  );
}
