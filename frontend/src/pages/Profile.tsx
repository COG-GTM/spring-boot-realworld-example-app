import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { ArticleCard } from '../components/ArticleCard';
import { useAuth } from '../hooks/useAuth';
import { profilesApi, articlesApi } from '../services/api';
import type { Profile as ProfileType, Article } from '../types';

export const Profile = () => {
  const { username } = useParams<{ username: string }>();
  const { user } = useAuth();
  
  const [profile, setProfile] = useState<ProfileType | null>(null);
  const [articles, setArticles] = useState<Article[]>([]);
  const [activeTab, setActiveTab] = useState<'articles' | 'favorites'>('articles');
  const [isLoading, setIsLoading] = useState(true);
  const [isFollowLoading, setIsFollowLoading] = useState(false);

  useEffect(() => {
    if (username) {
      loadProfile();
      loadArticles();
    }
  }, [username, activeTab]);

  const loadProfile = async () => {
    if (!username) return;
    
    try {
      const profileData = await profilesApi.getProfile(username);
      setProfile(profileData);
    } catch (error) {
      console.error('Error loading profile:', error);
    }
  };

  const loadArticles = async () => {
    if (!username) return;
    
    setIsLoading(true);
    try {
      const params = activeTab === 'articles' 
        ? { author: username }
        : { favorited: username };
      
      const articlesData = await articlesApi.getArticles(params);
      setArticles(articlesData.articles);
    } catch (error) {
      console.error('Error loading articles:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleFollow = async () => {
    if (!profile || !user || isFollowLoading) return;

    setIsFollowLoading(true);
    try {
      const updatedProfile = profile.following
        ? await profilesApi.unfollowUser(profile.username)
        : await profilesApi.followUser(profile.username);
      
      setProfile(updatedProfile);
    } catch (error) {
      console.error('Error toggling follow:', error);
    } finally {
      setIsFollowLoading(false);
    }
  };

  const handleArticleUpdate = (updatedArticle: Article) => {
    setArticles(articles.map(article => 
      article.slug === updatedArticle.slug ? updatedArticle : article
    ));
  };

  if (!profile) {
    return (
      <div className="text-center py-8">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-green-500 mx-auto"></div>
      </div>
    );
  }

  const isOwnProfile = user && user.username === profile.username;

  return (
    <div>
      <div className="bg-gray-100 py-8">
        <div className="max-w-4xl mx-auto px-4 text-center">
          <img
            src={profile.image || 'https://static.productionready.io/images/smiley-cyrus.jpg'}
            alt={profile.username}
            className="w-24 h-24 rounded-full mx-auto mb-4"
          />
          <h1 className="text-3xl font-bold text-gray-900 mb-2">{profile.username}</h1>
          {profile.bio && (
            <p className="text-gray-600 mb-4">{profile.bio}</p>
          )}
          
          {user && (
            <div>
              {isOwnProfile ? (
                <Link
                  to="/settings"
                  className="px-4 py-2 border border-gray-400 text-gray-400 rounded hover:bg-gray-400 hover:text-gray-800"
                >
                  Edit Profile Settings
                </Link>
              ) : (
                <button
                  onClick={handleFollow}
                  disabled={isFollowLoading}
                  className={`px-4 py-2 border rounded ${
                    profile.following
                      ? 'border-gray-400 text-gray-400 hover:bg-gray-400 hover:text-gray-800'
                      : 'border-gray-400 text-gray-400 hover:bg-gray-400 hover:text-gray-800'
                  }`}
                >
                  {profile.following ? 'Unfollow' : 'Follow'} {profile.username}
                </button>
              )}
            </div>
          )}
        </div>
      </div>

      <div className="max-w-4xl mx-auto px-4 py-8">
        <div className="flex border-b mb-6">
          <button
            onClick={() => setActiveTab('articles')}
            className={`px-4 py-2 ${
              activeTab === 'articles'
                ? 'text-green-500 border-b-2 border-green-500'
                : 'text-gray-500 hover:text-gray-700'
            }`}
          >
            My Articles
          </button>
          <button
            onClick={() => setActiveTab('favorites')}
            className={`px-4 py-2 ${
              activeTab === 'favorites'
                ? 'text-green-500 border-b-2 border-green-500'
                : 'text-gray-500 hover:text-gray-700'
            }`}
          >
            Favorited Articles
          </button>
        </div>

        {isLoading ? (
          <div className="text-center py-8">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-green-500 mx-auto"></div>
          </div>
        ) : articles.length === 0 ? (
          <div className="text-center py-8 text-gray-500">
            No articles are here... yet.
          </div>
        ) : (
          <div>
            {articles.map((article) => (
              <ArticleCard
                key={article.slug}
                article={article}
                onUpdate={handleArticleUpdate}
              />
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
