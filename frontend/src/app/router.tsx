import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import { Layout, ProtectedRoute } from '@/shared/components';
import {
  HomePage,
  LoginPage,
  RegisterPage,
  ArticlePage,
  EditorPage,
  ProfilePage,
  SettingsPage,
} from '@/pages';

const router = createBrowserRouter([
  {
    path: '/',
    element: <Layout />,
    children: [
      {
        index: true,
        element: <HomePage />,
      },
      {
        path: 'login',
        element: <LoginPage />,
      },
      {
        path: 'register',
        element: <RegisterPage />,
      },
      {
        path: 'article/:slug',
        element: <ArticlePage />,
      },
      {
        path: 'editor',
        element: (
          <ProtectedRoute>
            <EditorPage />
          </ProtectedRoute>
        ),
      },
      {
        path: 'editor/:slug',
        element: (
          <ProtectedRoute>
            <EditorPage />
          </ProtectedRoute>
        ),
      },
      {
        path: 'profile/:username',
        element: <ProfilePage />,
      },
      {
        path: 'settings',
        element: (
          <ProtectedRoute>
            <SettingsPage />
          </ProtectedRoute>
        ),
      },
    ],
  },
]);

export function AppRouter() {
  return <RouterProvider router={router} />;
}
