export default function CarteStatistique({ titre, children }) {
  return (
    <section className="stat-card">
      <h2>{titre}</h2>
      {children}
    </section>
  );
}
