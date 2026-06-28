import { Navigate, Route, Routes } from 'react-router-dom';
import ProtectedBackofficeRoute from './components/ProtectedBackofficeRoute.jsx';
import BackofficeAccess from './pages/BackofficeAccess.jsx';
import BackofficeDashboard from './pages/BackofficeDashboard.jsx';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/backoffice/access" replace />} />
      <Route path="/backoffice/access" element={<BackofficeAccess />} />
      <Route
        path="/backoffice/dashboard"
        element={
          <ProtectedBackofficeRoute>
            <BackofficeDashboard />
          </ProtectedBackofficeRoute>
        }
      />
    </Routes>
  );
}
