import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { profileApi } from '../api/profileApi';

export const profileKeys = {
  all: ['profiles'] as const,
  detail: (username: string) => [...profileKeys.all, username] as const,
};

export function useProfile(username: string) {
  return useQuery({
    queryKey: profileKeys.detail(username),
    queryFn: () => profileApi.getProfile(username),
    enabled: !!username,
  });
}

export function useFollowUser() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ username, following }: { username: string; following: boolean }) =>
      following ? profileApi.unfollowUser(username) : profileApi.followUser(username),
    onSuccess: (_, { username }) => {
      queryClient.invalidateQueries({ queryKey: profileKeys.detail(username) });
    },
  });
}
