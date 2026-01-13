import api from './api';
import type {
  MarketDataDashboard,
  ForexData,
  BrazilianIndices,
  USIndices,
  CryptoData,
  InvestmentPortfolio,
  RiskProfile,
} from '../types';

export const marketService = {
  getDashboard: async (): Promise<MarketDataDashboard> => {
    const response = await api.get('/market/dashboard');
    return response.data;
  },

  getForex: async (): Promise<ForexData> => {
    const response = await api.get('/market/forex');
    return response.data;
  },

  getBrazilianIndices: async (): Promise<BrazilianIndices> => {
    const response = await api.get('/market/brazil');
    return response.data;
  },

  getUSIndices: async (): Promise<USIndices> => {
    const response = await api.get('/market/us');
    return response.data;
  },

  getCrypto: async (): Promise<CryptoData> => {
    const response = await api.get('/market/crypto');
    return response.data;
  },

  getPortfolios: async (): Promise<InvestmentPortfolio[]> => {
    const response = await api.get('/market/portfolios');
    return response.data;
  },

  // Personalized Portfolio
  generatePersonalizedPortfolio: async (
    userId: number,
    riskProfile: RiskProfile
  ): Promise<InvestmentPortfolio> => {
    const response = await api.post('/market/personalized-portfolio', {
      userId,
      riskProfile,
    });
    return response.data;
  },

  getPersonalizedPortfolio: async (userId: number): Promise<InvestmentPortfolio | null> => {
    try {
      const response = await api.get(`/market/personalized-portfolio/${userId}`);
      return response.data;
    } catch (error: unknown) {
      // 404 significa que nao tem carteira salva
      if (error && typeof error === 'object' && 'response' in error) {
        const axiosError = error as { response?: { status?: number } };
        if (axiosError.response?.status === 404) {
          return null;
        }
      }
      throw error;
    }
  },
};
