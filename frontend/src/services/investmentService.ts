import type { AssetAnalysis } from '../types';

const API_URL = 'http://localhost:8080/api/market';

export const investmentService = {
  async getAssetAnalysis(ticker: string, portfolioType: string): Promise<AssetAnalysis> {
    const response = await fetch(
      `${API_URL}/asset/${encodeURIComponent(ticker)}/analysis?portfolioType=${encodeURIComponent(portfolioType)}`
    );

    if (!response.ok) {
      throw new Error(`Failed to fetch asset analysis: ${response.status}`);
    }

    return response.json();
  },
};
