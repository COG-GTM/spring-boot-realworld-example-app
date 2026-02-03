import { apiClient } from '@/shared/api/client';
import { API_ENDPOINTS } from '@/shared/api/endpoints';
import type { CommentsResponse, CommentResponse } from '@/shared/types';

export const commentsApi = {
  getComments: async (slug: string): Promise<CommentsResponse> => {
    const { data } = await apiClient.get(API_ENDPOINTS.COMMENTS(slug));
    return data;
  },

  addComment: async (slug: string, body: string): Promise<CommentResponse> => {
    const { data } = await apiClient.post(API_ENDPOINTS.COMMENTS(slug), {
      comment: { body },
    });
    return data;
  },

  deleteComment: async (slug: string, id: number): Promise<void> => {
    await apiClient.delete(API_ENDPOINTS.COMMENT(slug, id));
  },
};
