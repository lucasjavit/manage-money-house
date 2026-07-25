import type { AssetAnalysis } from '../types';

// Caminho relativo: passa pelo proxy do nginx e funciona de qualquer dispositivo da rede.
const API_URL = '/api/market';

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
