import { useState } from 'react';
import { useAuthStore } from '@/features/auth/store/authStore';
import { useAddComment } from '../hooks/useComments';

interface CommentFormProps {
  slug: string;
}

export function CommentForm({ slug }: CommentFormProps) {
  const [body, setBody] = useState('');
  const user = useAuthStore((state) => state.user);
  const addCommentMutation = useAddComment();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!body.trim()) return;

    await addCommentMutation.mutateAsync({ slug, body });
    setBody('');
  };

  return (
    <form onSubmit={handleSubmit} className="border border-gray-200 rounded mb-4">
      <textarea
        placeholder="Write a comment..."
        value={body}
        onChange={(e) => setBody(e.target.value)}
        className="w-full p-4 border-b border-gray-200 resize-none focus:outline-none"
        rows={3}
      />
      <div className="bg-gray-50 px-4 py-3 flex items-center justify-between">
        <img
          src={user?.image || 'https://api.realworld.io/images/smiley-cyrus.jpeg'}
          alt={user?.username}
          className="w-8 h-8 rounded-full"
        />
        <button
          type="submit"
          disabled={addCommentMutation.isPending || !body.trim()}
          className="bg-green-500 text-white px-4 py-2 rounded text-sm hover:bg-green-600 disabled:opacity-50"
        >
          {addCommentMutation.isPending ? 'Posting...' : 'Post Comment'}
        </button>
      </div>
    </form>
  );
}
