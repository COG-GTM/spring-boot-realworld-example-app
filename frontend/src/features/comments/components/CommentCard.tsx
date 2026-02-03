import { Link } from 'react-router-dom';
import { Trash2 } from 'lucide-react';
import { formatDate } from '@/shared/utils';
import { useAuthStore } from '@/features/auth/store/authStore';
import { useDeleteComment } from '../hooks/useComments';
import type { Comment } from '@/shared/types';

interface CommentCardProps {
  comment: Comment;
  slug: string;
}

export function CommentCard({ comment, slug }: CommentCardProps) {
  const user = useAuthStore((state) => state.user);
  const deleteCommentMutation = useDeleteComment();

  const isAuthor = user?.username === comment.author.username;

  const handleDelete = () => {
    if (window.confirm('Are you sure you want to delete this comment?')) {
      deleteCommentMutation.mutate({ slug, id: comment.id });
    }
  };

  return (
    <div className="border border-gray-200 rounded mb-3">
      <div className="p-4">
        <p className="text-gray-800">{comment.body}</p>
      </div>
      <div className="bg-gray-50 px-4 py-3 flex items-center justify-between border-t border-gray-200">
        <div className="flex items-center gap-2">
          <Link to={`/profile/${comment.author.username}`}>
            <img
              src={comment.author.image || 'https://api.realworld.io/images/smiley-cyrus.jpeg'}
              alt={comment.author.username}
              className="w-5 h-5 rounded-full"
            />
          </Link>
          <Link
            to={`/profile/${comment.author.username}`}
            className="text-green-500 text-sm hover:underline"
          >
            {comment.author.username}
          </Link>
          <span className="text-gray-400 text-xs">{formatDate(comment.createdAt)}</span>
        </div>
        {isAuthor && (
          <button
            onClick={handleDelete}
            disabled={deleteCommentMutation.isPending}
            className="text-gray-400 hover:text-red-500 disabled:opacity-50"
          >
            <Trash2 size={14} />
          </button>
        )}
      </div>
    </div>
  );
}
