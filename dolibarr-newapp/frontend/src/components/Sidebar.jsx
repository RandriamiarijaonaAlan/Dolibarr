import { NavLink } from 'react-router-dom';
import { oublierCodeBackoffice } from '../services/backofficeService.js';
import { useNavigate } from 'react-router-dom';

export default function Sidebar() {
  const navigate = useNavigate();

  function quitter() {
    oublierCodeBackoffice();
    navigate('/backoffice/access');
  }

  return (
    <aside className="sidebar">
      <div className="sidebar-brand">
        <span className="sidebar-brand-icon">⚙</span>
        <span className="sidebar-brand-name">Backoffice</span>
      </div>

      <nav className="sidebar-nav">
        <NavLink
          to="/backoffice/dashboard/statistiques"
          className={({ isActive }) => 'sidebar-link' + (isActive ? ' sidebar-link--active' : '')}
        >
          <span className="sidebar-link-icon">📊</span>
          Statistiques
        </NavLink>
        <NavLink
          to="/backoffice/dashboard/import"
          className={({ isActive }) => 'sidebar-link' + (isActive ? ' sidebar-link--active' : '')}
        >
          <span className="sidebar-link-icon">📥</span>
          Import
        </NavLink>
        <NavLink
          to="/backoffice/jours-feries"
          className={({ isActive }) => 'sidebar-link' + (isActive ? ' sidebar-link--active' : '')}
        >
          <span className="sidebar-link-icon">JF</span>
          Jours feries
        </NavLink>
        <NavLink
          to="/backoffice/dashboard/reinitialisation"
          className={({ isActive }) => 'sidebar-link' + (isActive ? ' sidebar-link--active' : '')}
        >
          <span className="sidebar-link-icon">🔄</span>
          Réinitialisation
        </NavLink>
      </nav>

      <div className="sidebar-footer">
        <button className="sidebar-quit" onClick={quitter}>
          Quitter
        </button>
      </div>
    </aside>
  );
}
