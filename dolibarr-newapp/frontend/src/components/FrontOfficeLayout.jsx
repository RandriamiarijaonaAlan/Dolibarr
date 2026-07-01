import { NavLink, Outlet } from 'react-router-dom';

/** Mise en page du front-office : barre de navigation supérieure + contenu (Outlet). */
export default function FrontOfficeLayout() {
  const classeLien = ({ isActive }) => 'fo-nav-lien' + (isActive ? ' fo-nav-lien--actif' : '');

  return (
    <div className="fo-layout">
      <header className="fo-header">
        <span className="fo-brand">💼 Gestion des salaires</span>
        <nav className="fo-nav">
          <NavLink to="/salaries" end className={classeLien}>
            Salariés
          </NavLink>
          <NavLink to="/salaries/creer" className={classeLien}>
            Créer un salaire
          </NavLink>
          <NavLink to="/frontoffice/salaries" className={classeLien}>
            Liste salaries
          </NavLink>
          <NavLink to="/frontoffice/generation-salaires" className={classeLien}>
            Generation salaires
          </NavLink>
          <NavLink to="/backoffice/access" className="fo-nav-lien fo-nav-lien--secondaire">
            Backoffice
          </NavLink>
        </nav>
      </header>
      <main className="fo-contenu">
        <Outlet />
      </main>
    </div>
  );
}
