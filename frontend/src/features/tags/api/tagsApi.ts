import { apiClient } from '@/shared/api/client';
import { API_ENDPOINTS } from '@/shared/api/endpoints';
import type { TagsResponse } from '@/shared/types';

export const tagsApi = {
  getTags: async (): Promise<TagsResponse> => {
    const { data } = await apiClient.get(API_ENDPOINTS.TAGS);
    return data;
  },
};
