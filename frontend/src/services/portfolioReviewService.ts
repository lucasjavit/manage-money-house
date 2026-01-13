import api from './api';
import type {
  PortfolioAnalysis,
  PortfolioReviewStatus,
  PortfolioReviewResult,
} from '../types';

export const portfolioReviewService = {
  // Run full review (all portfolios)
  runFullReview: async (): Promise<PortfolioReviewResult> => {
    const response = await api.post('/portfolio-review/run');
    return response.data;
  },

  // Run review for a specific portfolio
  runPortfolioReview: async (portfolioName: string): Promise<PortfolioReviewResult> => {
    const response = await api.post(`/portfolio-review/run/${encodeURIComponent(portfolioName)}`);
    return response.data;
  },

  // Run review for a specific asset
  runAssetReview: async (portfolioName: string, ticker: string): Promise<PortfolioReviewResult> => {
    const response = await api.post(
      `/portfolio-review/run/${encodeURIComponent(portfolioName)}/${encodeURIComponent(ticker)}`
    );
    return response.data;
  },

  // Get execution status
  getStatus: async (): Promise<PortfolioReviewStatus> => {
    const response = await api.get('/portfolio-review/status');
    return response.data;
  },

  // Get all active analyses
  getAllAnalyses: async (): Promise<PortfolioAnalysis[]> => {
    const response = await api.get('/portfolio-review/analyses');
    return response.data;
  },

  // Get analyses for a specific portfolio
  getPortfolioAnalyses: async (portfolioName: string): Promise<PortfolioAnalysis[]> => {
    const response = await api.get(
      `/portfolio-review/analyses/${encodeURIComponent(portfolioName)}`
    );
    return response.data;
  },

  // Get latest analysis for a ticker
  getTickerAnalysis: async (ticker: string): Promise<PortfolioAnalysis> => {
    const response = await api.get(
      `/portfolio-review/analyses/ticker/${encodeURIComponent(ticker)}`
    );
    return response.data;
  },

  // Get analysis history for a ticker
  getTickerHistory: async (ticker: string): Promise<PortfolioAnalysis[]> => {
    const response = await api.get(
      `/portfolio-review/history/${encodeURIComponent(ticker)}`
    );
    return response.data;
  },

  // Get assets recommended for substitution
  getSubstitutions: async (): Promise<PortfolioAnalysis[]> => {
    const response = await api.get('/portfolio-review/substitutions');
    return response.data;
  },
};
