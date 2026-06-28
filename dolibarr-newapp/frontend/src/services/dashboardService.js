import { envoyerRequete } from './apiService.js';

export async function recupererDashboard() {
  return envoyerRequete('/api/backoffice/dashboard');
}
