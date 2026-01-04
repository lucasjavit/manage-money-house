import api from './api';
import type { Salary, SalaryRequest, SalaryCalculationRequest, SalaryCalculationResponse, AnnualSalaryCalculationResponse } from '../types';

export const salaryService = {
  async getSalaryByUser(userId: number): Promise<Salary | null> {
    try {
      const response = await api.get<Salary>(`/salaries/user/${userId}`);
      return response.data;
    } catch (error: any) {
      if (error.response?.status === 404) {
        return null;
      }
      throw error;
    }
  },

  async createOrUpdateSalary(request: SalaryRequest): Promise<Salary> {
    const response = await api.post<Salary>('/salaries', request);
    return response.data;
  },

  async calculateVariableSalary(request: SalaryCalculationRequest): Promise<SalaryCalculationResponse> {
    const response = await api.post<SalaryCalculationResponse>('/salaries/calculate', request);
    return response.data;
  },

  async calculateAnnualSalary(userId: number, year: number): Promise<AnnualSalaryCalculationResponse> {
    const response = await api.get<AnnualSalaryCalculationResponse>(`/salaries/calculate/annual?userId=${userId}&year=${year}`);
    return response.data;
  },

  async deleteSalary(id: number): Promise<void> {
    await api.delete(`/salaries/${id}`);
  },
};

