export default function TableauStatistique({ titre, colonnes, lignes, messageVide = 'Aucune donnée disponible' }) {
  return (
    <section className="stat-table-section">
      <h3>{titre}</h3>
      {lignes.length === 0 ? (
        <p className="empty-message">{messageVide}</p>
      ) : (
        <div className="table-wrapper">
          <table className="stat-table">
            <thead>
              <tr>
                {colonnes.map((colonne) => (
                  <th key={colonne.cle}>{colonne.libelle}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {lignes.map((ligne, index) => (
                <tr key={`${titre}-${index}`}>
                  {colonnes.map((colonne) => (
                    <td key={colonne.cle}>{colonne.formater ? colonne.formater(ligne[colonne.cle]) : ligne[colonne.cle]}</td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
