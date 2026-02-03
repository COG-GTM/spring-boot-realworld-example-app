import { apiClient } from '@/shared/api/client';
import { API_ENDPOINTS } from '@/shared/api/endpoints';
import type { ProfileResponse } from '@/shared/types';

export const profileApi = {
  getProfile: async (username: string): Promise<ProfileResponse> => {
    const { data } = await apiClient.get(API_ENDPOINTS.PROFILE(username));
    return data;
  },

  followUser: async (username: string): Promise<ProfileResponse> => {
    const { data } = await apiClient.post(API_ENDPOINTS.FOLLOW(username));
    return data;
  },

  unfollowUser: async (username: string): Promise<ProfileResponse> => {
    const { data } = await apiClient.delete(API_ENDPOINTS.FOLLOW(username));
    return data;
  },
};
