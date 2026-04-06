import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export const Settings = () => {
  const { user, updateUser, logout } = useAuth();
  const navigate = useNavigate();
  
  const [image, setImage] = useState('');
  const [username, setUsername] = useState('');
  const [bio, setBio] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

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
    setError('');
    setIsLoading(true);

    try {
      const updateData: any = {
        image: image || undefined,
        username,
        bio: bio || undefined,
        email,
      };

      if (password) {
        updateData.password = password;
      }

      await updateUser(updateData);
      navigate(`/profile/${username}`);
    } catch (err: any) {
      const errors = err.response?.data?.errors;
      if (errors) {
        const errorMessages = Object.entries(errors)
          .map(([field, messages]: [string, any]) => `${field}: ${messages.join(', ')}`)
          .join('; ');
        setError(errorMessages);
      } else {
        setError('Failed to update profile');
      }
    } finally {
      setIsLoading(false);
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  if (!user) {
    return null;
  }

  return (
    <div className="max-w-md mx-auto mt-8">
      <h1 className="text-3xl font-bold text-gray-900 text-center mb-8">Your Settings</h1>

      {error && (
        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-4">
        <input
          type="url"
          placeholder="URL of profile picture"
          value={image}
          onChange={(e) => setImage(e.target.value)}
          className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:border-green-500"
        />
        
        <input
          type="text"
          placeholder="Username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          required
          className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:border-green-500"
        />
        
        <textarea
          placeholder="Short bio about you"
          value={bio}
          onChange={(e) => setBio(e.target.value)}
          rows={8}
          className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:border-green-500 resize-vertical"
        />
        
        <input
          type="email"
          placeholder="Email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
          className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:border-green-500"
        />
        
        <input
          type="password"
          placeholder="New Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:border-green-500"
        />
        
        <button
          type="submit"
          disabled={isLoading}
          className="w-full bg-green-500 text-white py-3 rounded-lg hover:bg-green-600 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {isLoading ? 'Updating...' : 'Update Settings'}
        </button>
      </form>

      <hr className="my-8" />

      <button
        onClick={handleLogout}
        className="w-full bg-red-500 text-white py-3 rounded-lg hover:bg-red-600"
      >
        Or click here to logout.
      </button>
    </div>
  );
};
