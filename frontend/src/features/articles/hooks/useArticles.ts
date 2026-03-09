import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { articlesApi } from '../api/articlesApi';
import type { ArticleParams } from '@/shared/types';

export const articleKeys = {
  all: ['articles'] as const,
  lists: () => [...articleKeys.all, 'list'] as const,
  list: (params: ArticleParams) => [...articleKeys.lists(), params] as const,
  feed: (params?: { limit?: number; offset?: number }) => [...articleKeys.all, 'feed', params] as const,
  details: () => [...articleKeys.all, 'detail'] as const,
  detail: (slug: string) => [...articleKeys.details(), slug] as const,
};

export function useArticles(params: ArticleParams = {}) {
  return useQuery({
    queryKey: articleKeys.list(params),
    queryFn: () => articlesApi.getArticles(params),
    staleTime: 1000 * 60,
  });
}

export function useFeed(params: { limit?: number; offset?: number } = {}) {
  return useQuery({
    queryKey: articleKeys.feed(params),
    queryFn: () => articlesApi.getFeed(params),
    staleTime: 1000 * 60,
  });
}

export function useArticle(slug: string) {
  return useQuery({
    queryKey: articleKeys.detail(slug),
    queryFn: () => articlesApi.getArticle(slug),
    enabled: !!slug,
  });
}

export function useCreateArticle() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  return useMutation({
    mutationFn: articlesApi.createArticle,
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: articleKeys.lists() });
      navigate(`/article/${data.article.slug}`);
    },
  });
}

export function useUpdateArticle() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  return useMutation({
    mutationFn: ({ slug, ...article }: { slug: string; title?: string; description?: string; body?: string }) =>
      articlesApi.updateArticle(slug, article),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: articleKeys.detail(data.article.slug) });
      queryClient.invalidateQueries({ queryKey: articleKeys.lists() });
      navigate(`/article/${data.article.slug}`);
    },
  });
}

export function useDeleteArticle() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  return useMutation({
    mutationFn: articlesApi.deleteArticle,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: articleKeys.lists() });
      navigate('/');
    },
  });
}

export function useFavoriteArticle() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ slug, favorited }: { slug: string; favorited: boolean }) =>
      favorited ? articlesApi.unfavoriteArticle(slug) : articlesApi.favoriteArticle(slug),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: articleKeys.all });
    },
  });
}
