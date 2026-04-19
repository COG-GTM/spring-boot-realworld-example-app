import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { articlesApi, commentsApi, profilesApi } from '../services/api';
import { CommentForm } from '../components/CommentForm';
import { CommentList } from '../components/CommentList';
import { useAuth } from '../hooks/useAuth';
import type { Article, Comment } from '../types';

export const ArticleView = () => {
  const { slug } = useParams<{ slug: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  
  const [article, setArticle] = useState<Article | null>(null);
  const [comments, setComments] = useState<Comment[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isFollowing, setIsFollowing] = useState(false);
  const [isFollowLoading, setIsFollowLoading] = useState(false);

  useEffect(() => {
    if (slug) {
      loadArticle();
      loadComments();
    }
  }, [slug]);

  const loadArticle = async () => {
    if (!slug) return;
    
    try {
      const articleData = await articlesApi.getArticle(slug);
      setArticle(articleData);
      setIsFollowing(articleData.author.following);
    } catch (error) {
      console.error('Error loading article:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const loadComments = async () => {
    if (!slug) return;
    
    try {
      const commentsData = await commentsApi.getComments(slug);
      setComments(commentsData);
    } catch (error) {
      console.error('Error loading comments:', error);
    }
  };

  const handleFavorite = async () => {
    if (!article || !user) return;

    try {
      const updatedArticle = article.favorited
        ? await articlesApi.unfavoriteArticle(article.slug)
        : await articlesApi.favoriteArticle(article.slug);
      setArticle(updatedArticle);
    } catch (error) {
      console.error('Error toggling favorite:', error);
    }
  };

  const handleFollow = async () => {
    if (!article || !user || isFollowLoading) return;

    setIsFollowLoading(true);
    try {
      const updatedProfile = isFollowing
        ? await profilesApi.unfollowUser(article.author.username)
        : await profilesApi.followUser(article.author.username);
      
      setIsFollowing(updatedProfile.following);
      setArticle({
        ...article,
        author: { ...article.author, following: updatedProfile.following }
      });
    } catch (error) {
      console.error('Error toggling follow:', error);
    } finally {
      setIsFollowLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!article || !user || article.author.username !== user.username) return;
    
    if (window.confirm('Are you sure you want to delete this article?')) {
      try {
        await articlesApi.deleteArticle(article.slug);
        navigate('/');
      } catch (error) {
        console.error('Error deleting article:', error);
      }
    }
  };

  const handleCommentSubmit = async (body: string) => {
    if (!slug) return;
    
    const newComment = await commentsApi.addComment(slug, body);
    setComments([newComment, ...comments]);
  };

  const handleCommentDelete = async (commentId: string) => {
    if (!slug) return;
    
    await commentsApi.deleteComment(slug, commentId);
    setComments(comments.filter(comment => comment.id !== commentId));
  };

  if (isLoading) {
    return (
      <div className="text-center py-8">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-green-500 mx-auto"></div>
      </div>
    );
  }

  if (!article) {
    return (
      <div className="text-center py-8">
        <h1 className="text-2xl font-bold text-gray-900">Article not found</h1>
      </div>
    );
  }

  const isAuthor = user && user.username === article.author.username;

  return (
    <div>
      <div className="bg-gray-800 text-white py-8">
        <div className="max-w-4xl mx-auto px-4">
          <h1 className="text-4xl font-bold mb-4">{article.title}</h1>
          
          <div className="flex items-center justify-between">
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
                  className="text-white hover:underline"
                >
                  {article.author.username}
                </Link>
                <p className="text-gray-300 text-sm">
                  {new Date(article.createdAt).toLocaleDateString('en-US', {
                    month: 'long',
                    day: 'numeric',
                    year: 'numeric'
                  })}
                </p>
              </div>
            </div>

            {user && (
              <div className="flex items-center space-x-2">
                {isAuthor ? (
                  <>
                    <Link
                      to={`/editor/${article.slug}`}
                      className="px-4 py-2 border border-gray-400 text-gray-400 rounded hover:bg-gray-400 hover:text-gray-800"
                    >
                      Edit Article
                    </Link>
                    <button
                      onClick={handleDelete}
                      className="px-4 py-2 border border-red-500 text-red-500 rounded hover:bg-red-500 hover:text-white"
                    >
                      Delete Article
                    </button>
                  </>
                ) : (
                  <>
                    <button
                      onClick={handleFollow}
                      disabled={isFollowLoading}
                      className={`px-4 py-2 border rounded ${
                        isFollowing
                          ? 'border-gray-400 text-gray-400 hover:bg-gray-400 hover:text-gray-800'
                          : 'border-gray-400 text-gray-400 hover:bg-gray-400 hover:text-gray-800'
                      }`}
                    >
                      {isFollowing ? 'Unfollow' : 'Follow'} {article.author.username}
                    </button>
                    <button
                      onClick={handleFavorite}
                      className={`px-4 py-2 border rounded flex items-center ${
                        article.favorited
                          ? 'border-green-500 text-green-500 hover:bg-green-500 hover:text-white'
                          : 'border-green-500 text-green-500 hover:bg-green-500 hover:text-white'
                      }`}
                    >
                      <svg className="w-4 h-4 mr-1" fill="currentColor" viewBox="0 0 20 20">
                        <path fillRule="evenodd" d="M3.172 5.172a4 4 0 015.656 0L10 6.343l1.172-1.171a4 4 0 115.656 5.656L10 17.657l-6.828-6.829a4 4 0 010-5.656z" clipRule="evenodd" />
                      </svg>
                      Favorite Article ({article.favoritesCount})
                    </button>
                  </>
                )}
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="max-w-4xl mx-auto px-4 py-8">
        <div className="prose max-w-none mb-8">
          <p className="text-lg text-gray-600 mb-6">{article.description}</p>
          <div className="whitespace-pre-wrap text-gray-800">{article.body}</div>
        </div>

        {article.tagList.length > 0 && (
          <div className="flex flex-wrap gap-1 mb-8">
            {article.tagList.map((tag) => (
              <span
                key={tag}
                className="px-2 py-1 text-xs bg-gray-200 text-gray-600 rounded-full"
              >
                {tag}
              </span>
            ))}
          </div>
        )}

        <hr className="my-8" />

        <div className="space-y-6">
          <CommentForm onSubmit={handleCommentSubmit} />
          <CommentList comments={comments} onDelete={handleCommentDelete} />
        </div>
      </div>
    </div>
  );
};
