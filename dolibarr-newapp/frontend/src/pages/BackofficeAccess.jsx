import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ActionButton from '../components/ActionButton.jsx';
import { obtenirCodeUniqueParDefaut, verifierCode } from '../services/backofficeService.js';

export default function BackofficeAccess() {
  const navigate = useNavigate();
  const [code, setCode] = useState(obtenirCodeUniqueParDefaut());
  const [message, setMessage] = useState('');
  const [chargement, setChargement] = useState(false);

  async function soumettreCode(event) {
    event.preventDefault();
    setChargement(true);
    setMessage('');

    try {
      const resultat = await verifierCode(code);

      if (resultat.autorise) {
        navigate('/backoffice/dashboard');
        return;
      }

      setMessage(resultat.message);
    } catch (erreur) {
      setMessage(erreur.message);
    } finally {
      setChargement(false);
    }
  }

  return (
    <main className="page page--centered">
      <section className="access-panel">
        <h1>Backoffice</h1>
        <form className="form" onSubmit={soumettreCode}>
          <label htmlFor="code">Code unique</label>
          <input
            id="code"
            type="text"
            value={code}
            onChange={(event) => setCode(event.target.value)}
          />
          <ActionButton type="submit" disabled={chargement}>
            {chargement ? 'Vérification...' : 'Accéder'}
          </ActionButton>
        </form>
        {message && <p className="message message--error">{message}</p>}
      </section>
    </main>
  );
}
