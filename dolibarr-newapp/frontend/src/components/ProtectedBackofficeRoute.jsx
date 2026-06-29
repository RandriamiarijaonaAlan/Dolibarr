import { Navigate } from 'react-router-dom';
import { estBackofficeAutorise } from '../services/backofficeService.js';

export default function ProtectedBackofficeRoute({ children }) {
  if (!estBackofficeAutorise()) {
    return <Navigate to="/backoffice/access" replace />;
  }

  return children;
}
