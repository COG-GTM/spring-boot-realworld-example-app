import React, { useState } from 'react';
import { useAuth } from '../hooks/useAuth';

interface CommentFormProps {
  onSubmit: (body: string) => Promise<void>;
}

export const CommentForm = ({ onSubmit }: CommentFormProps) => {
  const { user } = useAuth();
  const [body, setBody] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (!user) {
    return null;
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!body.trim() || isSubmitting) return;

    setIsSubmitting(true);
    try {
      await onSubmit(body);
      setBody('');
    } catch (error) {
      console.error('Error submitting comment:', error);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="border border-gray-200 rounded">
      <textarea
        value={body}
        onChange={(e) => setBody(e.target.value)}
        placeholder="Write a comment..."
        className="w-full p-4 border-0 resize-none focus:outline-none"
        rows={3}
      />
      <div className="flex justify-between items-center px-4 py-3 bg-gray-50 border-t">
        <div className="flex items-center">
          <img
            src={user.image || 'https://static.productionready.io/images/smiley-cyrus.jpg'}
            alt={user.username}
            className="w-8 h-8 rounded-full mr-2"
          />
          <span className="text-sm text-gray-600">{user.username}</span>
        </div>
        <button
          type="submit"
          disabled={!body.trim() || isSubmitting}
          className="px-4 py-2 bg-green-500 text-white rounded text-sm hover:bg-green-600 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {isSubmitting ? 'Posting...' : 'Post Comment'}
        </button>
      </div>
    </form>
  );
};
