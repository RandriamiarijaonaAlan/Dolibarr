import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import ActionButton from '../components/ActionButton.jsx';
import { recupererDetailSalarieFrontoffice } from '../services/salarieService.js';

export default function DetailSalariePage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [detail, setDetail] = useState(null);
  const [chargement, setChargement] = useState(false);
  const [erreur, setErreur] = useState('');

  useEffect(() => {
    chargerDetailSalarie();
  }, [id]);

  async function chargerDetailSalarie() {
    setChargement(true);
    setErreur('');

    try {
      const reponse = await recupererDetailSalarieFrontoffice(id);
      if (!reponse?.salarie) {
        setErreur('Salarie introuvable');
        setDetail(null);
      } else {
        setDetail(reponse);
      }
    } catch (err) {
      setErreur(err.message || 'Impossible de charger le detail du salarie');
    } finally {
      setChargement(false);
    }
  }

  function afficherStatutPaiement(statut) {
    if (statut === 'Paye') return 'statut--solde';
    if (statut === 'Partiel') return 'statut--partiel';
    return 'statut--impaye';
  }

  function formaterMontant(montant) {
    return new Intl.NumberFormat('fr-FR', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(Number(montant || 0));
  }

  function formaterDate(date) {
    if (!date) return '-';
    const correspondance = /^(\d{4})-(\d{2})-(\d{2})/.exec(date);
    return correspondance ? `${correspondance[3]}/${correspondance[2]}/${correspondance[1]}` : date;
  }

  function retournerListe() {
    navigate('/frontoffice/salaries');
  }

  return (
    <div className="fo-page">
      <div className="fo-page-entete">
        <div>
          <h1>Detail salarie</h1>
          <p>{detail?.salarie?.nom || `Salarie #${id}`}</p>
        </div>
        <ActionButton variant="secondary" onClick={retournerListe}>
          Retour liste salaries
        </ActionButton>
      </div>

      {chargement && <p className="empty-message">Chargement...</p>}
      {erreur && <p className="message message--error">{erreur}</p>}

      {!chargement && !erreur && detail?.salarie && (
        <>
          <section className="salarie-fiche">
            <div><span>Reference employe</span><strong>{detail.salarie.refEmploye || '-'}</strong></div>
            <div><span>Nom</span><strong>{detail.salarie.nom || '-'}</strong></div>
            <div><span>Prenom</span><strong>{detail.salarie.prenom || '-'}</strong></div>
            <div><span>Identifiant</span><strong>{detail.salarie.identifiant || '-'}</strong></div>
            <div><span>Poste</span><strong>{detail.salarie.poste || '-'}</strong></div>
            <div><span>Genre</span><strong>{detail.salarie.genre || '-'}</strong></div>
            <div><span>Heures de travail</span><strong>{detail.salarie.heureTravailSemaine ?? '-'}</strong></div>
            <div><span>Statut</span><strong>{detail.salarie.statut || '-'}</strong></div>
          </section>

          <section className="salarie-historique">
            <h2>Historique des salaires</h2>
            {detail.historiquesSalaires.length === 0 ? (
              <p className="empty-message">Aucun salaire pour ce salarie.</p>
            ) : (
              detail.historiquesSalaires.map((salaire) => (
                <article className="salarie-salaire" key={salaire.idSalaire}>
                  <div className="table-wrapper">
                    <table className="stat-table fo-table">
                      <thead>
                        <tr>
                          <th>Reference salaire</th>
                          <th>Date debut</th>
                          <th>Date fin</th>
                          <th>Montant salaire</th>
                          <th>Total paye</th>
                          <th>Reste a payer</th>
                          <th>Statut paiement</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr>
                          <td>{salaire.refSalaire || salaire.idSalaire}</td>
                          <td>{formaterDate(salaire.dateDebut)}</td>
                          <td>{formaterDate(salaire.dateFin)}</td>
                          <td>{formaterMontant(salaire.montantSalaire)}</td>
                          <td>{formaterMontant(salaire.totalPaye)}</td>
                          <td><strong>{formaterMontant(salaire.resteAPayer)}</strong></td>
                          <td>
                            <span className={`fo-statut ${afficherStatutPaiement(salaire.statutPaiement)}`}>
                              {salaire.statutPaiement}
                            </span>
                          </td>
                        </tr>
                      </tbody>
                    </table>
                  </div>

                  <h3>Paiements</h3>
                  <div className="table-wrapper">
                    <table className="stat-table fo-table">
                      <thead>
                        <tr>
                          <th>Date paiement</th>
                          <th>Montant paiement</th>
                          <th>Reference paiement</th>
                        </tr>
                      </thead>
                      <tbody>
                        {salaire.paiements.map((paiement) => (
                          <tr key={paiement.idPaiement || `${paiement.datePaiement}-${paiement.montantPaiement}`}>
                            <td>{formaterDate(paiement.datePaiement)}</td>
                            <td>{formaterMontant(paiement.montantPaiement)}</td>
                            <td>{paiement.referencePaiement || paiement.idPaiement || '-'}</td>
                          </tr>
                        ))}
                        {salaire.paiements.length === 0 && (
                          <tr>
                            <td colSpan="3" className="empty-message">Aucun paiement pour ce salaire.</td>
                          </tr>
                        )}
                      </tbody>
                    </table>
                  </div>
                </article>
              ))
            )}
          </section>
        </>
      )}
    </div>
  );
}
