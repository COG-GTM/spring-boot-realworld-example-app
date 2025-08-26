import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import type { Article } from '../types';
import { articlesApi } from '../services/api';
import { useAuth } from '../hooks/useAuth';

interface ArticleCardProps {
  article: Article;
  onUpdate?: (updatedArticle: Article) => void;
}

export const ArticleCard = ({ article, onUpdate }: ArticleCardProps) => {
  const { user } = useAuth();
  const [isLoading, setIsLoading] = useState(false);

  const handleFavorite = async (e: React.MouseEvent) => {
    e.preventDefault();
    if (!user || isLoading) return;

    setIsLoading(true);
    try {
      const updatedArticle = article.favorited
        ? await articlesApi.unfavoriteArticle(article.slug)
        : await articlesApi.favoriteArticle(article.slug);
      
      onUpdate?.(updatedArticle);
    } catch (error) {
      console.error('Error toggling favorite:', error);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="border-t border-gray-200 py-6">
      <div className="flex justify-between items-start mb-4">
        <div className="flex items-center">
          <Link to={`/profile/${article.author.username}`}>
            <img
              src={article.author.image || 'https://static.productionready.io/images/smiley-cyrus.jpg'}
              alt={article.author.username}
              className="w-8 h-8 rounded-full mr-2"
            />
          </Link>
          <div>
            <Link
              to={`/profile/${article.author.username}`}
              className="text-green-500 font-medium hover:underline"
            >
              {article.author.username}
            </Link>
            <p className="text-gray-400 text-sm">
              {new Date(article.createdAt).toLocaleDateString('en-US', {
                month: 'long',
                day: 'numeric',
                year: 'numeric'
              })}
            </p>
          </div>
        </div>
        
        {user && (
          <button
            onClick={handleFavorite}
            disabled={isLoading}
            className={`flex items-center px-2 py-1 text-sm border rounded ${
              article.favorited
                ? 'bg-green-500 text-white border-green-500'
                : 'text-green-500 border-green-500 hover:bg-green-500 hover:text-white'
            } ${isLoading ? 'opacity-50 cursor-not-allowed' : ''}`}
          >
            <svg className="w-4 h-4 mr-1" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M3.172 5.172a4 4 0 015.656 0L10 6.343l1.172-1.171a4 4 0 115.656 5.656L10 17.657l-6.828-6.829a4 4 0 010-5.656z" clipRule="evenodd" />
            </svg>
            {article.favoritesCount}
          </button>
        )}
      </div>

      <Link to={`/article/${article.slug}`} className="block">
        <h2 className="text-xl font-semibold text-gray-900 mb-2 hover:text-gray-700">
          {article.title}
        </h2>
        <p className="text-gray-600 mb-4">
          {article.description}
        </p>
      </Link>

      <div className="flex justify-between items-center">
        <Link
          to={`/article/${article.slug}`}
          className="text-gray-400 text-sm hover:text-gray-600"
        >
          Read more...
        </Link>
        
        <div className="flex flex-wrap gap-1">
          {article.tagList.map((tag) => (
            <span
              key={tag}
              className="px-2 py-1 text-xs bg-gray-200 text-gray-600 rounded-full"
            >
              {tag}
            </span>
          ))}
        </div>
      </div>
    </div>
  );
};
