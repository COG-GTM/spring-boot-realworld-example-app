import { useQuery } from '@tanstack/react-query';
import { tagsApi } from '../api/tagsApi';

export const tagKeys = {
  all: ['tags'] as const,
};

export function useTags() {
  return useQuery({
    queryKey: tagKeys.all,
    queryFn: tagsApi.getTags,
    staleTime: 1000 * 60 * 5,
  });
}
