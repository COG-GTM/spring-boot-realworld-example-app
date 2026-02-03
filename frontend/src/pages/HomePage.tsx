import { useState } from 'react';
import { useArticles, useFeed } from '@/features/articles/hooks/useArticles';
import { ArticleList, FeedTabs, Pagination } from '@/features/articles/components';
import { TagList } from '@/features/tags/components';
import { useAuthStore } from '@/features/auth/store/authStore';

const ARTICLES_PER_PAGE = 10;

export function HomePage() {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const [activeTab, setActiveTab] = useState<'global' | 'feed' | 'tag'>(
    isAuthenticated ? 'feed' : 'global'
  );
  const [selectedTag, setSelectedTag] = useState<string | undefined>();
  const [currentPage, setCurrentPage] = useState(1);

  const offset = (currentPage - 1) * ARTICLES_PER_PAGE;

  const globalArticles = useArticles(
    activeTab === 'global'
      ? { limit: ARTICLES_PER_PAGE, offset }
      : activeTab === 'tag' && selectedTag
      ? { tag: selectedTag, limit: ARTICLES_PER_PAGE, offset }
      : { limit: 0 }
  );

  const feedArticles = useFeed(
    activeTab === 'feed' ? { limit: ARTICLES_PER_PAGE, offset } : { limit: 0 }
  );

  const currentData = activeTab === 'feed' ? feedArticles : globalArticles;
  const totalPages = Math.ceil((currentData.data?.articlesCount || 0) / ARTICLES_PER_PAGE);

  const handleTabChange = (tab: 'global' | 'feed' | 'tag') => {
    setActiveTab(tab);
    setSelectedTag(undefined);
    setCurrentPage(1);
  };

  const handleTagClick = (tag: string) => {
    setSelectedTag(tag);
    setActiveTab('tag');
    setCurrentPage(1);
  };

  const handlePageChange = (page: number) => {
    setCurrentPage(page);
  };

  return (
    <div>
      <div className="bg-green-500 text-white py-8 text-center mb-8">
        <h1 className="text-5xl font-bold mb-2">conduit</h1>
        <p className="text-xl">A place to share your knowledge.</p>
      </div>

      <div className="max-w-6xl mx-auto px-4">
        <div className="flex gap-8">
          <div className="flex-1">
            <FeedTabs
              activeTab={activeTab}
              selectedTag={selectedTag}
              onTabChange={handleTabChange}
            />
            <ArticleList
              articles={currentData.data?.articles}
              isLoading={currentData.isLoading}
            />
            <Pagination
              currentPage={currentPage}
              totalPages={totalPages}
              onPageChange={handlePageChange}
            />
          </div>

          <div className="w-64">
            <div className="bg-gray-100 p-4 rounded">
              <h3 className="mb-2 font-medium">Popular Tags</h3>
              <TagList onTagClick={handleTagClick} />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
