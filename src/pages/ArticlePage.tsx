import { Link, useParams, useNavigate } from 'react-router-dom';
import ReactMarkdown from 'react-markdown';
import { Heart, Plus, Minus, Edit, Trash2 } from 'lucide-react';
import { useArticle, useFavoriteArticle, useDeleteArticle } from '@/features/articles/hooks/useArticles';
import { useFollowUser } from '@/features/profile/hooks/useProfile';
import { CommentList } from '@/features/comments/components';
import { useAuthStore } from '@/features/auth/store/authStore';
import { formatDate } from '@/shared/utils';

export function ArticlePage() {
  const { slug } = useParams<{ slug: string }>();
  const navigate = useNavigate();
  const { user, isAuthenticated } = useAuthStore();

  const { data, isLoading } = useArticle(slug || '');
  const favoriteMutation = useFavoriteArticle();
  const followMutation = useFollowUser();
  const deleteMutation = useDeleteArticle();

  if (isLoading) {
    return (
      <div className="max-w-6xl mx-auto px-4 py-8">
        <div className="text-gray-500">Loading article...</div>
      </div>
    );
  }

  if (!data?.article) {
    return (
      <div className="max-w-6xl mx-auto px-4 py-8">
        <div className="text-gray-500">Article not found.</div>
      </div>
    );
  }

  const article = data.article;
  const isAuthor = user?.username === article.author.username;

  const handleFavorite = () => {
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }
    favoriteMutation.mutate({ slug: article.slug, favorited: article.favorited });
  };

  const handleFollow = () => {
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }
    followMutation.mutate({ username: article.author.username, following: article.author.following });
  };

  const handleDelete = () => {
    if (window.confirm('Are you sure you want to delete this article?')) {
      deleteMutation.mutate(article.slug);
    }
  };

  return (
    <div>
      <div className="bg-gray-800 text-white py-8">
        <div className="max-w-6xl mx-auto px-4">
          <h1 className="text-4xl font-bold mb-8">{article.title}</h1>

          <div className="flex items-center gap-6">
            <div className="flex items-center gap-2">
              <Link to={`/profile/${article.author.username}`}>
                <img
                  src={article.author.image || 'https://api.realworld.io/images/smiley-cyrus.jpeg'}
                  alt={article.author.username}
                  className="w-8 h-8 rounded-full"
                />
              </Link>
              <div>
                <Link
                  to={`/profile/${article.author.username}`}
                  className="text-white hover:underline"
                >
                  {article.author.username}
                </Link>
                <p className="text-gray-400 text-xs">{formatDate(article.createdAt)}</p>
              </div>
            </div>

            {isAuthor ? (
              <div className="flex gap-2">
                <Link
                  to={`/editor/${article.slug}`}
                  className="flex items-center gap-1 px-3 py-1 border border-gray-400 text-gray-400 rounded text-sm hover:bg-gray-700"
                >
                  <Edit size={14} />
                  Edit Article
                </Link>
                <button
                  onClick={handleDelete}
                  disabled={deleteMutation.isPending}
                  className="flex items-center gap-1 px-3 py-1 border border-red-500 text-red-500 rounded text-sm hover:bg-red-500 hover:text-white disabled:opacity-50"
                >
                  <Trash2 size={14} />
                  Delete Article
                </button>
              </div>
            ) : (
              <div className="flex gap-2">
                <button
                  onClick={handleFollow}
                  disabled={followMutation.isPending}
                  className={`flex items-center gap-1 px-3 py-1 border rounded text-sm ${
                    article.author.following
                      ? 'border-gray-400 text-gray-400 hover:bg-gray-700'
                      : 'border-gray-400 text-gray-400 hover:bg-gray-700'
                  } disabled:opacity-50`}
                >
                  {article.author.following ? <Minus size={14} /> : <Plus size={14} />}
                  {article.author.following ? 'Unfollow' : 'Follow'} {article.author.username}
                </button>
                <button
                  onClick={handleFavorite}
                  disabled={favoriteMutation.isPending}
                  className={`flex items-center gap-1 px-3 py-1 border rounded text-sm ${
                    article.favorited
                      ? 'bg-green-500 text-white border-green-500'
                      : 'border-green-500 text-green-500 hover:bg-green-500 hover:text-white'
                  } disabled:opacity-50`}
                >
                  <Heart size={14} fill={article.favorited ? 'currentColor' : 'none'} />
                  {article.favorited ? 'Unfavorite' : 'Favorite'} Article ({article.favoritesCount})
                </button>
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="max-w-6xl mx-auto px-4 py-8">
        <div className="prose max-w-none mb-8">
          <ReactMarkdown>{article.body}</ReactMarkdown>
        </div>

        {article.tagList.length > 0 && (
          <ul className="flex flex-wrap gap-1 mb-8 pb-8 border-b border-gray-200">
            {article.tagList.map((tag) => (
              <li
                key={tag}
                className="px-2 py-0.5 text-xs text-gray-400 border border-gray-300 rounded-full"
              >
                {tag}
              </li>
            ))}
          </ul>
        )}

        <CommentList slug={article.slug} />
      </div>
    </div>
  );
}
