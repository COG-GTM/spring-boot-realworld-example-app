import { useEffect } from 'react';
import { Providers, AppRouter } from './app';
import { useAuthStore } from './features/auth/store/authStore';
import { setAuthToken } from './shared/api/client';

function AppContent() {
  const token = useAuthStore((state) => state.token);

  useEffect(() => {
    if (token) {
      setAuthToken(token);
    }
  }, [token]);

  return <AppRouter />;
}

function App() {
  return (
    <Providers>
      <AppContent />
    </Providers>
  );
}

export default App;
