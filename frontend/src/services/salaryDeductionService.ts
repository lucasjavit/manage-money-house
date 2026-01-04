import api from './api';
import type { BoletoProcessResponse, SalaryDeduction, SalaryDeductionRequest } from '../types';
import type { ExtractUploadRequest } from './extractService';

export const salaryDeductionService = {
  async processBoleto(request: ExtractUploadRequest): Promise<BoletoProcessResponse> {
    const response = await api.post<BoletoProcessResponse>('/salary-deductions/process', request);
    return response.data;
  },

  async createDeduction(request: SalaryDeductionRequest): Promise<SalaryDeduction> {
    const response = await api.post<SalaryDeduction>('/salary-deductions', request);
    return response.data;
  },

  async getDeductions(userId: number, month: number, year: number): Promise<SalaryDeduction[]> {
    const response = await api.get<SalaryDeduction[]>(`/salary-deductions?userId=${userId}&month=${month}&year=${year}`);
    return response.data;
  },

  async deleteDeduction(id: number): Promise<void> {
    await api.delete(`/salary-deductions/${id}`);
  },
};

