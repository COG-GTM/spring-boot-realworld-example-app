import { useState, useEffect } from 'react';
import { useUpdateUser, useLogout } from '@/features/auth/hooks/useAuth';
import { useAuthStore } from '@/features/auth/store/authStore';
import { FormErrors } from '@/shared/components/FormErrors';
import axios from 'axios';

export function SettingsPage() {
  const user = useAuthStore((state) => state.user);
  const updateMutation = useUpdateUser();
  const logout = useLogout();

  const [image, setImage] = useState('');
  const [username, setUsername] = useState('');
  const [bio, setBio] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<{ [key: string]: string[] } | undefined>();

  useEffect(() => {
    if (user) {
      setImage(user.image || '');
      setUsername(user.username);
      setBio(user.bio || '');
      setEmail(user.email);
    }
  }, [user]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrors(undefined);

    try {
      const updateData: {
        email?: string;
        username?: string;
        password?: string;
        image?: string | null;
        bio?: string | null;
      } = {
        email,
        username,
        image: image || null,
        bio: bio || null,
      };

      if (password) {
        updateData.password = password;
      }

      await updateMutation.mutateAsync(updateData);
      setPassword('');
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
      <h1 className="text-4xl font-bold text-center mb-6">Your Settings</h1>

      <FormErrors errors={errors} />

      <form onSubmit={handleSubmit}>
        <div className="mb-4">
          <input
            type="text"
            placeholder="URL of profile picture"
            value={image}
            onChange={(e) => setImage(e.target.value)}
            className="w-full px-4 py-3 border border-gray-300 rounded focus:outline-none focus:border-green-500"
          />
        </div>
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
          <textarea
            placeholder="Short bio about you"
            value={bio}
            onChange={(e) => setBio(e.target.value)}
            className="w-full px-4 py-3 border border-gray-300 rounded focus:outline-none focus:border-green-500 resize-none"
            rows={8}
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
            placeholder="New Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="w-full px-4 py-3 border border-gray-300 rounded focus:outline-none focus:border-green-500"
          />
        </div>
        <button
          type="submit"
          disabled={updateMutation.isPending}
          className="w-full bg-green-500 text-white py-3 px-4 rounded hover:bg-green-600 disabled:opacity-50"
        >
          {updateMutation.isPending ? 'Updating...' : 'Update Settings'}
        </button>
      </form>

      <hr className="my-6" />

      <button
        onClick={logout}
        className="w-full border border-red-500 text-red-500 py-3 px-4 rounded hover:bg-red-500 hover:text-white"
      >
        Or click here to logout.
      </button>
    </div>
  );
}
