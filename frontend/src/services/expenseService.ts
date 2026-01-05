import type { Expense, ExpenseRequest, ExpenseType } from '../types';
import api from './api';

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

  createExpenseType: async (name: string): Promise<ExpenseType> => {
    const response = await api.post<ExpenseType>('/expense-types', { name });
    return response.data;
  },

  deleteExpenseType: async (id: number): Promise<void> => {
    await api.delete(`/expense-types/${id}`);
  },
};

