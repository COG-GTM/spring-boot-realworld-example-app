import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import type { UserWithToken } from '../types';
import { authApi } from '../services/api';

interface AuthContextType {
  user: UserWithToken | null;
  login: (email: string, password: string) => Promise<void>;
  register: (username: string, email: string, password: string) => Promise<void>;
  logout: () => void;
  updateUser: (userData: Partial<UserWithToken>) => Promise<void>;
  isLoading: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

interface AuthProviderProps {
  children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [user, setUser] = useState<UserWithToken | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (token) {
      authApi.getCurrentUser()
        .then(setUser)
        .catch(() => {
          localStorage.removeItem('token');
        })
        .finally(() => setIsLoading(false));
    } else {
      setIsLoading(false);
    }
  }, []);

  const login = async (email: string, password: string) => {
    const userData = await authApi.login({ email, password });
    localStorage.setItem('token', userData.token);
    setUser(userData);
  };

  const register = async (username: string, email: string, password: string) => {
    const userData = await authApi.register({ username, email, password });
    localStorage.setItem('token', userData.token);
    setUser(userData);
  };

  const logout = () => {
    localStorage.removeItem('token');
    setUser(null);
  };

  const updateUser = async (userData: Partial<UserWithToken>) => {
    const updatedUser = await authApi.updateUser(userData);
    setUser(updatedUser);
  };

  return React.createElement(
    AuthContext.Provider,
    { value: { user, login, register, logout, updateUser, isLoading } },
    children
  );
};
