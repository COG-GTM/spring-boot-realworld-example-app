import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { setAuthToken } from '@/shared/api/client';

interface User {
  email: string;
  username: string;
  bio: string | null;
  image: string | null;
}

interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  setAuth: (user: User, token: string) => void;
  clearAuth: () => void;
  updateUser: (user: Partial<User>) => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      token: null,
      isAuthenticated: false,

      setAuth: (user, token) => {
        setAuthToken(token);
        set({
          user,
          token,
          isAuthenticated: true,
        });
      },

      clearAuth: () => {
        setAuthToken(null);
        set({
          user: null,
          token: null,
          isAuthenticated: false,
        });
      },

      updateUser: (updates) =>
        set((state) => ({
          user: state.user ? { ...state.user, ...updates } : null,
        })),
    }),
    {
      name: 'conduit-auth',
    }
  )
);
