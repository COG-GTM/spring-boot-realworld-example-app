import React, { useState, useEffect } from 'react';
import { ArticleCard } from '../components/ArticleCard';
import { TagList } from '../components/TagList';
import { useAuth } from '../hooks/useAuth';
import { articlesApi, tagsApi } from '../services/api';
import type { Article } from '../types';

export const Home = () => {
  const { user } = useAuth();
  const [articles, setArticles] = useState<Article[]>([]);
  const [tags, setTags] = useState<string[]>([]);
  const [selectedTag, setSelectedTag] = useState<string>('');
  const [activeTab, setActiveTab] = useState<'global' | 'feed'>('global');
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    loadTags();
  }, []);

  useEffect(() => {
    loadArticles();
  }, [activeTab, selectedTag]);

  const loadTags = async () => {
    try {
      const tagsData = await tagsApi.getTags();
      setTags(tagsData);
    } catch (error) {
      console.error('Error loading tags:', error);
    }
  };

  const loadArticles = async () => {
    setIsLoading(true);
    try {
      let articlesData;
      if (activeTab === 'feed' && user) {
        articlesData = await articlesApi.getFeed();
      } else {
        articlesData = await articlesApi.getArticles(
          selectedTag ? { tag: selectedTag } : {}
        );
      }
      setArticles(articlesData.articles);
    } catch (error) {
      console.error('Error loading articles:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleTagSelect = (tag: string) => {
    setSelectedTag(selectedTag === tag ? '' : tag);
    setActiveTab('global');
  };

  const handleArticleUpdate = (updatedArticle: Article) => {
    setArticles(articles.map(article => 
      article.slug === updatedArticle.slug ? updatedArticle : article
    ));
  };

  return (
    <div className="max-w-6xl mx-auto px-4 py-8">
      <div className="bg-green-500 text-white text-center py-16 mb-8 rounded-lg">
        <h1 className="text-5xl font-bold mb-4">conduit</h1>
        <p className="text-xl">A place to share your knowledge.</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
        <div className="lg:col-span-3">
          <div className="flex border-b mb-6">
            {user && (
              <button
                onClick={() => setActiveTab('feed')}
                className={`px-4 py-2 ${
                  activeTab === 'feed'
                    ? 'text-green-500 border-b-2 border-green-500'
                    : 'text-gray-500 hover:text-gray-700'
                }`}
              >
                Your Feed
              </button>
            )}
            <button
              onClick={() => setActiveTab('global')}
              className={`px-4 py-2 ${
                activeTab === 'global'
                  ? 'text-green-500 border-b-2 border-green-500'
                  : 'text-gray-500 hover:text-gray-700'
              }`}
            >
              Global Feed
            </button>
            {selectedTag && (
              <button
                onClick={() => setSelectedTag('')}
                className="px-4 py-2 text-green-500 border-b-2 border-green-500"
              >
                #{selectedTag}
              </button>
            )}
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

        <div className="lg:col-span-1">
          <TagList
            tags={tags}
            selectedTag={selectedTag}
            onTagSelect={handleTagSelect}
          />
        </div>
      </div>
    </div>
  );
};
