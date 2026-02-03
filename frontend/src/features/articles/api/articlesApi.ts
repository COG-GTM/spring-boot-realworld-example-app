import { apiClient } from '@/shared/api/client';
import { API_ENDPOINTS } from '@/shared/api/endpoints';
import type { ArticlesResponse, ArticleResponse, ArticleParams } from '@/shared/types';

interface CreateArticleInput {
  title: string;
  description: string;
  body: string;
  tagList?: string[];
}

interface UpdateArticleInput {
  title?: string;
  description?: string;
  body?: string;
}

export const articlesApi = {
  getArticles: async (params: ArticleParams = {}): Promise<ArticlesResponse> => {
    const { data } = await apiClient.get(API_ENDPOINTS.ARTICLES, { params });
    return data;
  },

  getFeed: async (params: { limit?: number; offset?: number } = {}): Promise<ArticlesResponse> => {
    const { data } = await apiClient.get(API_ENDPOINTS.ARTICLES_FEED, { params });
    return data;
  },

  getArticle: async (slug: string): Promise<ArticleResponse> => {
    const { data } = await apiClient.get(API_ENDPOINTS.ARTICLE(slug));
    return data;
  },

  createArticle: async (article: CreateArticleInput): Promise<ArticleResponse> => {
    const { data } = await apiClient.post(API_ENDPOINTS.ARTICLES, { article });
    return data;
  },

  updateArticle: async (slug: string, article: UpdateArticleInput): Promise<ArticleResponse> => {
    const { data } = await apiClient.put(API_ENDPOINTS.ARTICLE(slug), { article });
    return data;
  },

  deleteArticle: async (slug: string): Promise<void> => {
    await apiClient.delete(API_ENDPOINTS.ARTICLE(slug));
  },

  favoriteArticle: async (slug: string): Promise<ArticleResponse> => {
    const { data } = await apiClient.post(API_ENDPOINTS.FAVORITE(slug));
    return data;
  },

  unfavoriteArticle: async (slug: string): Promise<ArticleResponse> => {
    const { data } = await apiClient.delete(API_ENDPOINTS.FAVORITE(slug));
    return data;
  },
};
