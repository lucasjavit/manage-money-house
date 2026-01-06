import api from './api';
import type { SalaryConversionProcessRequest, SalaryConversionProcessResponse, SalaryConversionRequest, SalaryConversionResponse } from '../types';

export const salaryConversionService = {
  async processConversionText(request: SalaryConversionProcessRequest): Promise<SalaryConversionProcessResponse> {
    const response = await api.post<SalaryConversionProcessResponse>('/salary-conversions/process', request);
    return response.data;
  },

  async createOrUpdateConversion(request: SalaryConversionRequest): Promise<SalaryConversionResponse> {
    const response = await api.post<SalaryConversionResponse>('/salary-conversions', request);
    return response.data;
  },

  async getConversion(userId: number, month: number, year: number): Promise<SalaryConversionResponse | null> {
    try {
      const response = await api.get<SalaryConversionResponse>(`/salary-conversions?userId=${userId}&month=${month}&year=${year}`);
      return response.data;
    } catch (error: any) {
      if (error.response?.status === 404) {
        return null;
      }
      throw error;
    }
  },
};

