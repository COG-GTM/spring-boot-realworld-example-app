import { apiClient } from '@/shared/api/client';
import { API_ENDPOINTS } from '@/shared/api/endpoints';
import type { UserResponse } from '@/shared/types';

interface LoginInput {
  email: string;
  password: string;
}

interface RegisterInput {
  username: string;
  email: string;
  password: string;
}

interface UpdateUserInput {
  email?: string;
  username?: string;
  password?: string;
  image?: string | null;
  bio?: string | null;
}

export const authApi = {
  login: async (credentials: LoginInput): Promise<UserResponse> => {
    const { data } = await apiClient.post(API_ENDPOINTS.LOGIN, {
      user: credentials,
    });
    return data;
  },

  register: async (userData: RegisterInput): Promise<UserResponse> => {
    const { data } = await apiClient.post(API_ENDPOINTS.REGISTER, {
      user: userData,
    });
    return data;
  },

  getCurrentUser: async (): Promise<UserResponse> => {
    const { data } = await apiClient.get(API_ENDPOINTS.CURRENT_USER);
    return data;
  },

  updateUser: async (userData: UpdateUserInput): Promise<UserResponse> => {
    const { data } = await apiClient.put(API_ENDPOINTS.CURRENT_USER, {
      user: userData,
    });
    return data;
  },
};
