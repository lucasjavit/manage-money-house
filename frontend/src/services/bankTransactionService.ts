import type { BankTransaction } from '../types';
import api from './api';

export const bankTransactionService = {
  list: async (userId: number, year: number, month: number): Promise<BankTransaction[]> => {
    const response = await api.get<BankTransaction[]>(
      `/bank-transactions?userId=${userId}&year=${year}&month=${month}`
    );
    return response.data;
  },

  // Completa um pendente (informa o valor) ou ajusta a descrição.
  update: async (
    id: number,
    data: { amount?: number; description?: string }
  ): Promise<BankTransaction> => {
    const response = await api.patch<BankTransaction>(`/bank-transactions/${id}`, data);
    return response.data;
  },

  remove: async (id: number): Promise<void> => {
    await api.delete(`/bank-transactions/${id}`);
  },
};
