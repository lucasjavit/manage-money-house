import api from './api';
import type { AIMonthlyAnalysis, Pattern } from '../types';

export const aiService = {
  /**
   * Gera análise mensal completa usando IA
   */
  analyzeMonth: async (userId: number, month: number, year: number): Promise<AIMonthlyAnalysis> => {
    const response = await api.get<AIMonthlyAnalysis>(
      `/ai/analyze?userId=${userId}&month=${month}&year=${year}`
    );
    return response.data;
  },

  /**
   * Detecta padrões de gastos
   */
  detectPatterns: async (userId: number, month: number, year: number): Promise<Pattern[]> => {
    const response = await api.get<Pattern[]>(
      `/ai/patterns?userId=${userId}&month=${month}&year=${year}`
    );
    return response.data;
  },
};
