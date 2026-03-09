import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useRegister } from '../hooks/useAuth';
import { FormErrors } from '@/shared/components/FormErrors';
import axios from 'axios';

export function RegisterForm() {
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<{ [key: string]: string[] } | undefined>();

  const registerMutation = useRegister();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrors(undefined);

    try {
      await registerMutation.mutateAsync({ username, email, password });
    } catch (error) {
      if (axios.isAxiosError(error) && error.response?.data?.errors) {
        setErrors(error.response.data.errors);
      } else {
        setErrors({ error: ['An unexpected error occurred'] });
      }
    }
  };

  return (
    <div className="max-w-md mx-auto px-4 py-8">
      <h1 className="text-4xl font-bold text-center mb-2">Sign up</h1>
      <p className="text-center mb-6">
        <Link to="/login" className="text-green-500 hover:underline">
          Have an account?
        </Link>
      </p>

      <FormErrors errors={errors} />

      <form onSubmit={handleSubmit}>
        <div className="mb-4">
          <input
            type="text"
            placeholder="Username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            className="w-full px-4 py-3 border border-gray-300 rounded focus:outline-none focus:border-green-500"
            required
          />
        </div>
        <div className="mb-4">
          <input
            type="email"
            placeholder="Email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="w-full px-4 py-3 border border-gray-300 rounded focus:outline-none focus:border-green-500"
            required
          />
        </div>
        <div className="mb-4">
          <input
            type="password"
            placeholder="Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="w-full px-4 py-3 border border-gray-300 rounded focus:outline-none focus:border-green-500"
            required
          />
        </div>
        <button
          type="submit"
          disabled={registerMutation.isPending}
          className="w-full bg-green-500 text-white py-3 px-4 rounded hover:bg-green-600 disabled:opacity-50 float-right"
        >
          {registerMutation.isPending ? 'Signing up...' : 'Sign up'}
        </button>
      </form>
    </div>
  );
}
