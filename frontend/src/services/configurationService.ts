import api from './api';
import type { Configuration } from '../types';

export const configurationService = {
  async getAllConfigurations(): Promise<Configuration[]> {
    const response = await api.get<Configuration[]>('/configurations');
    return response.data;
  },

  async getConfiguration(key: string): Promise<Configuration | null> {
    try {
      const response = await api.get<Configuration>(`/configurations/${key}`);
      return response.data;
    } catch (error) {
      return null;
    }
  },

  async saveOrUpdateConfiguration(config: Configuration): Promise<Configuration> {
    const response = await api.post<Configuration>('/configurations', config);
    return response.data;
  },

  async deleteConfiguration(key: string): Promise<void> {
    await api.delete(`/configurations/${key}`);
  },
};

