import axios from 'axios';
import type {
  UserWithToken,
  Profile,
  Article,
  Comment,
  LoginCredentials,
  RegisterData,
  NewArticle,
  UpdateUser,
  ArticlesParams
} from '../types';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers = config.headers || {};
    config.headers.Authorization = `Token ${token}`;
  }
  return config;
});

export const authApi = {
  login: async (credentials: LoginCredentials): Promise<UserWithToken> => {
    const response = await api.post('/users/login', { user: credentials });
    return (response.data as any).user;
  },

  register: async (userData: RegisterData): Promise<UserWithToken> => {
    const response = await api.post('/users', { user: userData });
    return (response.data as any).user;
  },

  getCurrentUser: async (): Promise<UserWithToken> => {
    const response = await api.get('/user');
    return (response.data as any).user;
  },

  updateUser: async (userData: UpdateUser): Promise<UserWithToken> => {
    const response = await api.put('/user', { user: userData });
    return (response.data as any).user;
  },
};

export const articlesApi = {
  getArticles: async (params: ArticlesParams = {}): Promise<{ articles: Article[]; articlesCount: number }> => {
    const response = await api.get('/articles', { params });
    return response.data as { articles: Article[]; articlesCount: number };
  },

  getFeed: async (limit = 20, offset = 0): Promise<{ articles: Article[]; articlesCount: number }> => {
    const response = await api.get('/articles/feed', { params: { limit, offset } });
    return response.data as { articles: Article[]; articlesCount: number };
  },

  getArticle: async (slug: string): Promise<Article> => {
    const response = await api.get(`/articles/${slug}`);
    return (response.data as any).article;
  },

  createArticle: async (article: NewArticle): Promise<Article> => {
    const response = await api.post('/articles', { article });
    return (response.data as any).article;
  },

  updateArticle: async (slug: string, article: Partial<NewArticle>): Promise<Article> => {
    const response = await api.put(`/articles/${slug}`, { article });
    return (response.data as any).article;
  },

  deleteArticle: async (slug: string): Promise<void> => {
    await api.delete(`/articles/${slug}`);
  },

  favoriteArticle: async (slug: string): Promise<Article> => {
    const response = await api.post(`/articles/${slug}/favorite`);
    return (response.data as any).article;
  },

  unfavoriteArticle: async (slug: string): Promise<Article> => {
    const response = await api.delete(`/articles/${slug}/favorite`);
    return (response.data as any).article;
  },
};

export const commentsApi = {
  getComments: async (slug: string): Promise<Comment[]> => {
    const response = await api.get(`/articles/${slug}/comments`);
    return (response.data as any).comments;
  },

  addComment: async (slug: string, body: string): Promise<Comment> => {
    const response = await api.post(`/articles/${slug}/comments`, { comment: { body } });
    return (response.data as any).comment;
  },

  deleteComment: async (slug: string, commentId: string): Promise<void> => {
    await api.delete(`/articles/${slug}/comments/${commentId}`);
  },
};

export const profilesApi = {
  getProfile: async (username: string): Promise<Profile> => {
    const response = await api.get(`/profiles/${username}`);
    return (response.data as any).profile;
  },

  followUser: async (username: string): Promise<Profile> => {
    const response = await api.post(`/profiles/${username}/follow`);
    return (response.data as any).profile;
  },

  unfollowUser: async (username: string): Promise<Profile> => {
    const response = await api.delete(`/profiles/${username}/follow`);
    return (response.data as any).profile;
  },
};

export const tagsApi = {
  getTags: async (): Promise<string[]> => {
    const response = await api.get('/tags');
    return (response.data as any).tags;
  },
};

export default api;
