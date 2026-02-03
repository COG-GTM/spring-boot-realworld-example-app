import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { commentsApi } from '../api/commentsApi';

export const commentKeys = {
  all: ['comments'] as const,
  list: (slug: string) => [...commentKeys.all, slug] as const,
};

export function useComments(slug: string) {
  return useQuery({
    queryKey: commentKeys.list(slug),
    queryFn: () => commentsApi.getComments(slug),
    enabled: !!slug,
  });
}

export function useAddComment() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ slug, body }: { slug: string; body: string }) =>
      commentsApi.addComment(slug, body),
    onSuccess: (_, { slug }) => {
      queryClient.invalidateQueries({ queryKey: commentKeys.list(slug) });
    },
  });
}

export function useDeleteComment() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ slug, id }: { slug: string; id: number }) =>
      commentsApi.deleteComment(slug, id),
    onSuccess: (_, { slug }) => {
      queryClient.invalidateQueries({ queryKey: commentKeys.list(slug) });
    },
  });
}
