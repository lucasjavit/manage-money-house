import api from './api';
import type { ExtractProcessResponse, IdentifiedTransaction, ExtractTransaction } from '../types';

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
};

