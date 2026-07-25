import api from './api';
import type {
  CompanyCategory,
  CompanyCategoryRequest,
  CompanyExpense,
  CompanyExpenseRequest,
} from '../types';

export const companyExpenseService = {
  // Categorias
  async getCategories(userId: number): Promise<CompanyCategory[]> {
    const r = await api.get<CompanyCategory[]>(`/company-expenses/categories?userId=${userId}`);
    return r.data;
  },
  async createCategory(request: CompanyCategoryRequest): Promise<CompanyCategory> {
    const r = await api.post<CompanyCategory>('/company-expenses/categories', request);
    return r.data;
  },
  async deleteCategory(id: number): Promise<void> {
    await api.delete(`/company-expenses/categories/${id}`);
  },

  // Lançamentos
  async getByMonth(userId: number, year: number, month: number): Promise<CompanyExpense[]> {
    const r = await api.get<CompanyExpense[]>(
      `/company-expenses?userId=${userId}&year=${year}&month=${month}`
    );
    return r.data;
  },
  async create(request: CompanyExpenseRequest): Promise<CompanyExpense> {
    const r = await api.post<CompanyExpense>('/company-expenses', request);
    return r.data;
  },
  async update(id: number, request: CompanyExpenseRequest): Promise<CompanyExpense> {
    const r = await api.patch<CompanyExpense>(`/company-expenses/${id}`, request);
    return r.data;
  },
  async remove(id: number): Promise<void> {
    await api.delete(`/company-expenses/${id}`);
  },
};
