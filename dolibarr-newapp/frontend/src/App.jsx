import { Navigate, Route, Routes } from 'react-router-dom';
import DashboardLayout from './components/DashboardLayout.jsx';
import ProtectedBackofficeRoute from './components/ProtectedBackofficeRoute.jsx';
import BackofficeAccess from './pages/BackofficeAccess.jsx';
import PageReinitialisation from './pages/PageReinitialisation.jsx';
import PageStatistiques from './pages/PageStatistiques.jsx';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/backoffice/access" replace />} />
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
        <Route path="reinitialisation" element={<PageReinitialisation />} />
      </Route>
    </Routes>
  );
}
