import { Navigate, Route, Routes } from 'react-router-dom';
import DashboardLayout from './components/DashboardLayout.jsx';
import FrontOfficeLayout from './components/FrontOfficeLayout.jsx';
import ProtectedBackofficeRoute from './components/ProtectedBackofficeRoute.jsx';
import BackofficeAccess from './pages/BackofficeAccess.jsx';
import DetailSalariePage from './pages/DetailSalariePage.jsx';
import GenerationSalairesPage from './pages/GenerationSalairesPage.jsx';
import JoursFeriesPage from './pages/JoursFeriesPage.jsx';
import ListeSalariesPage from './pages/ListeSalariesPage.jsx';
import PageCreerSalaire from './pages/PageCreerSalaire.jsx';
import PageImport from './pages/PageImport.jsx';
import PagePayerSalaire from './pages/PagePayerSalaire.jsx';
import PageReinitialisation from './pages/PageReinitialisation.jsx';
import PageSalairesEmploye from './pages/PageSalairesEmploye.jsx';
import PageSalariesListe from './pages/PageSalariesListe.jsx';
import PageStatistiques from './pages/PageStatistiques.jsx';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/salaries" replace />} />

      <Route path="/salaries" element={<FrontOfficeLayout />}>
        <Route index element={<PageSalariesListe />} />
        <Route path="creer" element={<PageCreerSalaire />} />
        <Route path="employe/:id" element={<PageSalairesEmploye />} />
        <Route path=":id/payer" element={<PagePayerSalaire />} />
      </Route>
      <Route path="/frontoffice" element={<FrontOfficeLayout />}>
        <Route path="salaries" element={<ListeSalariesPage />} />
        <Route path="salaries/:id" element={<DetailSalariePage />} />
        <Route path="generation-salaires" element={<GenerationSalairesPage />} />
      </Route>

      <Route path="/backoffice/access" element={<BackofficeAccess />} />
      <Route
        path="/backoffice/dashboard"
        element={
          <ProtectedBackofficeRoute>
            <DashboardLayout />
          </ProtectedBackofficeRoute>
        }
      >
        <Route index element={<Navigate to="statistiques" replace />} />
        <Route path="statistiques" element={<PageStatistiques />} />
        <Route path="import" element={<PageImport />} />
        <Route path="jours-feries" element={<JoursFeriesPage />} />
        <Route path="reinitialisation" element={<PageReinitialisation />} />
      </Route>
      <Route
        path="/backoffice/jours-feries"
        element={
          <ProtectedBackofficeRoute>
            <DashboardLayout />
          </ProtectedBackofficeRoute>
        }
      >
        <Route index element={<JoursFeriesPage />} />
      </Route>
    </Routes>
  );
}
