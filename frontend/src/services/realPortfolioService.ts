import api from './api';
import type {
  B3ReportUploadRequest,
  B3ReportUploadResponse,
  RealPortfolioSummary,
} from '../types';

export const realPortfolioService = {
  uploadReport: async (request: B3ReportUploadRequest): Promise<B3ReportUploadResponse> => {
    const response = await api.post('/real-portfolio/upload', request);
    return response.data;
  },

  getLatestPortfolio: async (userId: number): Promise<RealPortfolioSummary | null> => {
    try {
      const response = await api.get(`/real-portfolio/${userId}`);
      return response.data;
    } catch (error: unknown) {
      if (error && typeof error === 'object' && 'response' in error) {
        const axiosError = error as { response?: { status?: number } };
        if (axiosError.response?.status === 404) {
          return null;
        }
      }
      throw error;
    }
  },

  getPortfolioHistory: async (userId: number): Promise<RealPortfolioSummary[]> => {
    const response = await api.get(`/real-portfolio/${userId}/history`);
    return response.data;
  },

  getPortfolioByMonthYear: async (
    userId: number,
    year: number,
    month: number
  ): Promise<RealPortfolioSummary | null> => {
    try {
      const response = await api.get(`/real-portfolio/${userId}/${year}/${month}`);
      return response.data;
    } catch (error: unknown) {
      if (error && typeof error === 'object' && 'response' in error) {
        const axiosError = error as { response?: { status?: number } };
        if (axiosError.response?.status === 404) {
          return null;
        }
      }
      throw error;
    }
  },

  analyzePortfolio: async (userId: number): Promise<string> => {
    const response = await api.post(`/real-portfolio/${userId}/analyze`);
    return response.data.analysis;
  },

  analyzeIndividualAssets: async (userId: number): Promise<{ message: string; timestamp: string }> => {
    const response = await api.post(`/real-portfolio/${userId}/analyze-assets`);
    return response.data;
  },
};
