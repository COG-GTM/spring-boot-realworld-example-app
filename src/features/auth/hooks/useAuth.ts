import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { authApi } from '../api/authApi';
import { useAuthStore } from '../store/authStore';

export function useLogin() {
  const setAuth = useAuthStore((state) => state.setAuth);
  const navigate = useNavigate();

  return useMutation({
    mutationFn: authApi.login,
    onSuccess: (data) => {
      const { user } = data;
      setAuth(
        {
          email: user.email,
          username: user.username,
          bio: user.bio,
          image: user.image,
        },
        user.token
      );
      navigate('/');
    },
  });
}

export function useRegister() {
  const setAuth = useAuthStore((state) => state.setAuth);
  const navigate = useNavigate();

  return useMutation({
    mutationFn: authApi.register,
    onSuccess: (data) => {
      const { user } = data;
      setAuth(
        {
          email: user.email,
          username: user.username,
          bio: user.bio,
          image: user.image,
        },
        user.token
      );
      navigate('/');
    },
  });
}

export function useCurrentUser() {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);

  return useQuery({
    queryKey: ['currentUser'],
    queryFn: authApi.getCurrentUser,
    enabled: isAuthenticated,
  });
}

export function useUpdateUser() {
  const queryClient = useQueryClient();
  const updateUser = useAuthStore((state) => state.updateUser);

  return useMutation({
    mutationFn: authApi.updateUser,
    onSuccess: (data) => {
      const { user } = data;
      updateUser({
        email: user.email,
        username: user.username,
        bio: user.bio,
        image: user.image,
      });
      queryClient.invalidateQueries({ queryKey: ['currentUser'] });
    },
  });
}

export function useLogout() {
  const clearAuth = useAuthStore((state) => state.clearAuth);
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  return () => {
    clearAuth();
    queryClient.clear();
    navigate('/');
  };
}
