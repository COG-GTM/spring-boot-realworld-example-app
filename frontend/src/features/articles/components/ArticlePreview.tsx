import { Link } from 'react-router-dom';
import { Heart } from 'lucide-react';
import { formatDate } from '@/shared/utils';
import { useFavoriteArticle } from '../hooks/useArticles';
import { useAuthStore } from '@/features/auth/store/authStore';
import type { Article } from '@/shared/types';

interface ArticlePreviewProps {
  article: Article;
}

export function ArticlePreview({ article }: ArticlePreviewProps) {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const favoriteMutation = useFavoriteArticle();

  const handleFavorite = () => {
    if (!isAuthenticated) return;
    favoriteMutation.mutate({ slug: article.slug, favorited: article.favorited });
  };

  return (
    <div className="py-6 border-t border-gray-200">
      <div className="flex justify-between items-start mb-4">
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
              className="text-green-500 hover:underline font-medium"
            >
              {article.author.username}
            </Link>
            <p className="text-gray-400 text-xs">{formatDate(article.createdAt)}</p>
          </div>
        </div>
        <button
          onClick={handleFavorite}
          disabled={favoriteMutation.isPending || !isAuthenticated}
          className={`flex items-center gap-1 px-2 py-1 border rounded text-sm ${
            article.favorited
              ? 'bg-green-500 text-white border-green-500'
              : 'text-green-500 border-green-500 hover:bg-green-500 hover:text-white'
          } disabled:opacity-50`}
        >
          <Heart size={14} fill={article.favorited ? 'currentColor' : 'none'} />
          {article.favoritesCount}
        </button>
      </div>

      <Link to={`/article/${article.slug}`} className="block">
        <h2 className="text-2xl font-semibold mb-1 hover:text-green-500">
          {article.title}
        </h2>
        <p className="text-gray-500 mb-4">{article.description}</p>
        <div className="flex justify-between items-center">
          <span className="text-gray-400 text-sm">Read more...</span>
          {article.tagList.length > 0 && (
            <ul className="flex flex-wrap gap-1">
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
        </div>
      </Link>
    </div>
  );
}
