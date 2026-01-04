import api from './api';
import type { RecurringExpense, RecurringExpenseRequest } from '../types';

export const recurringExpenseService = {
  async createRecurringExpense(request: RecurringExpenseRequest): Promise<RecurringExpense> {
    const response = await api.post<RecurringExpense>('/recurring-expenses', request);
    return response.data;
  },

  async updateRecurringExpense(id: number, request: RecurringExpenseRequest): Promise<RecurringExpense> {
    const response = await api.put<RecurringExpense>(`/recurring-expenses/${id}`, request);
    return response.data;
  },

  async deleteRecurringExpense(id: number): Promise<void> {
    await api.delete(`/recurring-expenses/${id}`);
  },

  async getRecurringExpenseById(id: number): Promise<RecurringExpense> {
    const response = await api.get<RecurringExpense>(`/recurring-expenses/${id}`);
    return response.data;
  },

  async getAllRecurringExpenses(): Promise<RecurringExpense[]> {
    const response = await api.get<RecurringExpense[]>('/recurring-expenses');
    return response.data;
  },
};

