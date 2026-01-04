import api from './api';
import type { User } from '../types';

export const authService = {
  login: async (email: string): Promise<User> => {
    const response = await api.post<User>('/auth/login', { email });
    return response.data;
  },
};

