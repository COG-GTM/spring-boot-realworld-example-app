import { Link } from 'react-router-dom';
import { useComments } from '../hooks/useComments';
import { useAuthStore } from '@/features/auth/store/authStore';
import { CommentCard } from './CommentCard';
import { CommentForm } from './CommentForm';

interface CommentListProps {
  slug: string;
}

export function CommentList({ slug }: CommentListProps) {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const { data, isLoading } = useComments(slug);

  return (
    <div className="max-w-2xl mx-auto">
      {isAuthenticated ? (
        <CommentForm slug={slug} />
      ) : (
        <p className="mb-4 text-center">
          <Link to="/login" className="text-green-500 hover:underline">
            Sign in
          </Link>{' '}
          or{' '}
          <Link to="/register" className="text-green-500 hover:underline">
            sign up
          </Link>{' '}
          to add comments on this article.
        </p>
      )}

      {isLoading ? (
        <div className="text-gray-500">Loading comments...</div>
      ) : data?.comments && data.comments.length > 0 ? (
        data.comments.map((comment) => (
          <CommentCard key={comment.id} comment={comment} slug={slug} />
        ))
      ) : (
        <div className="text-gray-500">No comments yet.</div>
      )}
    </div>
  );
}
