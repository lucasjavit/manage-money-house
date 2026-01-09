import api from './api';
import type { ExtractProcessResponse, IdentifiedTransaction, ExtractTransaction, ExpenseType, ExpenseInsightsResponse } from '../types';

export interface ExtractUploadRequest {
  fileName: string;
  fileContent: string; // Base64
  fileType: 'pdf' | 'png';
}

export interface SaveTransactionsRequest {
  userId: number;
  transactions: IdentifiedTransaction[];
}

export const extractService = {
  async processExtract(request: ExtractUploadRequest): Promise<ExtractProcessResponse> {
    const response = await api.post<ExtractProcessResponse>('/extract/process', request);
    return response.data;
  },

  async saveTransactions(request: SaveTransactionsRequest): Promise<{ saved: number; failed: number; errors: string[]; transactions?: ExtractTransaction[] }> {
    const response = await api.post<{ saved: number; failed: number; errors: string[]; transactions?: ExtractTransaction[] }>('/extract/save', request);
    return response.data;
  },

  async getTransactions(userId: number): Promise<ExtractTransaction[]> {
    const response = await api.get<ExtractTransaction[]>(`/extract/transactions?userId=${userId}`);
    return response.data;
  },

  async getTransactionsByDateRange(userId: number, startDate: string, endDate: string): Promise<ExtractTransaction[]> {
    const response = await api.get<ExtractTransaction[]>(`/extract/transactions?userId=${userId}&startDate=${startDate}&endDate=${endDate}`);
    return response.data;
  },

  async deleteTransaction(id: number): Promise<void> {
    await api.delete(`/extract/transactions/${id}`);
  },

  async deleteTransactions(ids: number[]): Promise<void> {
    await api.delete('/extract/transactions', { data: ids });
  },

  async updateTransactionType(id: number, expenseTypeId: number): Promise<ExtractTransaction> {
    const response = await api.put<ExtractTransaction>(`/extract/transactions/${id}/type`, { expenseTypeId });
    return response.data;
  },

  async updateTransactionsType(ids: number[], expenseTypeId: number): Promise<ExtractTransaction[]> {
    const response = await api.put<ExtractTransaction[]>('/extract/transactions/type', { ids, expenseTypeId });
    return response.data;
  },

  async getExtractExpenseTypes(): Promise<ExpenseType[]> {
    try {
      console.log('Chamando API: /extract-expense-types');
      const response = await api.get<ExpenseType[]>('/extract-expense-types');
      console.log('Resposta da API:', response.data);
      return response.data;
    } catch (error: any) {
      console.error('Erro ao buscar tipos de despesa do extrato:', error);
      console.error('Detalhes do erro:', error.response?.data || error.message);
      throw error;
    }
  },

  async getInsights(userId: number, month: number, year: number): Promise<ExpenseInsightsResponse> {
    const response = await api.get<ExpenseInsightsResponse>(`/extract/insights?userId=${userId}&month=${month}&year=${year}`);
    return response.data;
  },
};

