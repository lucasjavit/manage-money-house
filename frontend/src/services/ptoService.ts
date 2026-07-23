import api from './api';
import type {
  PtoConfig,
  PtoConfigRequest,
  PtoVacation,
  PtoVacationRequest,
  PtoBalance,
} from '../types';

export const ptoService = {
  async getConfig(userId: number): Promise<PtoConfig | null> {
    const r = await api.get<PtoConfig | null>(`/pto/config?userId=${userId}`);
    return r.data;
  },

  async saveConfig(request: PtoConfigRequest): Promise<PtoConfig> {
    const r = await api.post<PtoConfig>('/pto/config', request);
    return r.data;
  },

  async getVacations(userId: number): Promise<PtoVacation[]> {
    const r = await api.get<PtoVacation[]>(`/pto/vacations?userId=${userId}`);
    return r.data;
  },

  async createVacation(request: PtoVacationRequest): Promise<PtoVacation> {
    const r = await api.post<PtoVacation>('/pto/vacations', request);
    return r.data;
  },

  async deleteVacation(id: number): Promise<void> {
    await api.delete(`/pto/vacations/${id}`);
  },

  async getBalance(userId: number, date?: string): Promise<PtoBalance> {
    const q = date ? `&date=${date}` : '';
    const r = await api.get<PtoBalance>(`/pto/balance?userId=${userId}${q}`);
    return r.data;
  },
};
