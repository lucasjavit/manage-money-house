import api from './api';
import type { Expense, ExpenseRequest, ExpenseType } from '../types';

export const expenseService = {
  getExpenses: async (year: number): Promise<Expense[]> => {
    const response = await api.get<Expense[]>(`/expenses?year=${year}`);
    return response.data;
  },

  createOrUpdateExpense: async (request: ExpenseRequest): Promise<Expense> => {
    const response = await api.post<Expense>('/expenses', request);
    return response.data;
  },

  deleteExpense: async (id: number): Promise<void> => {
    await api.delete(`/expenses/${id}`);
  },

  getExpenseTypes: async (): Promise<ExpenseType[]> => {
    const response = await api.get<ExpenseType[]>('/expense-types');
    return response.data;
  },
};

