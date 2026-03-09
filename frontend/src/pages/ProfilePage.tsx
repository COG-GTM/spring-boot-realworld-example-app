import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { Plus, Minus, Settings } from 'lucide-react';
import { useProfile, useFollowUser } from '@/features/profile/hooks/useProfile';
import { useArticles } from '@/features/articles/hooks/useArticles';
import { ArticleList, Pagination } from '@/features/articles/components';
import { useAuthStore } from '@/features/auth/store/authStore';

const ARTICLES_PER_PAGE = 5;

export function ProfilePage() {
  const { username } = useParams<{ username: string }>();
  const { user, isAuthenticated } = useAuthStore();
  const [activeTab, setActiveTab] = useState<'my' | 'favorited'>('my');
  const [currentPage, setCurrentPage] = useState(1);

  const { data: profileData, isLoading: isLoadingProfile } = useProfile(username || '');
  const followMutation = useFollowUser();

  const offset = (currentPage - 1) * ARTICLES_PER_PAGE;

  const myArticles = useArticles(
    activeTab === 'my' && username
      ? { author: username, limit: ARTICLES_PER_PAGE, offset }
      : { limit: 0 }
  );

  const favoritedArticles = useArticles(
    activeTab === 'favorited' && username
      ? { favorited: username, limit: ARTICLES_PER_PAGE, offset }
      : { limit: 0 }
  );

  const currentData = activeTab === 'my' ? myArticles : favoritedArticles;
  const totalPages = Math.ceil((currentData.data?.articlesCount || 0) / ARTICLES_PER_PAGE);

  const handleTabChange = (tab: 'my' | 'favorited') => {
    setActiveTab(tab);
    setCurrentPage(1);
  };

  const handleFollow = () => {
    if (!profileData?.profile) return;
    followMutation.mutate({
      username: profileData.profile.username,
      following: profileData.profile.following,
    });
  };

  if (isLoadingProfile) {
    return (
      <div className="max-w-6xl mx-auto px-4 py-8">
        <div className="text-gray-500">Loading profile...</div>
      </div>
    );
  }

  if (!profileData?.profile) {
    return (
      <div className="max-w-6xl mx-auto px-4 py-8">
        <div className="text-gray-500">User not found.</div>
      </div>
    );
  }

  const profile = profileData.profile;
  const isOwnProfile = user?.username === profile.username;

  return (
    <div>
      <div className="bg-gray-100 py-8">
        <div className="max-w-6xl mx-auto px-4 text-center">
          <img
            src={profile.image || 'https://api.realworld.io/images/smiley-cyrus.jpeg'}
            alt={profile.username}
            className="w-24 h-24 rounded-full mx-auto mb-4"
          />
          <h1 className="text-2xl font-bold mb-2">{profile.username}</h1>
          {profile.bio && <p className="text-gray-600 mb-4">{profile.bio}</p>}

          {isOwnProfile ? (
            <Link
              to="/settings"
              className="inline-flex items-center gap-1 px-3 py-1 border border-gray-400 text-gray-600 rounded text-sm hover:bg-gray-200"
            >
              <Settings size={14} />
              Edit Profile Settings
            </Link>
          ) : isAuthenticated ? (
            <button
              onClick={handleFollow}
              disabled={followMutation.isPending}
              className={`inline-flex items-center gap-1 px-3 py-1 border rounded text-sm ${
                profile.following
                  ? 'border-gray-400 text-gray-600 hover:bg-gray-200'
                  : 'border-gray-400 text-gray-600 hover:bg-gray-200'
              } disabled:opacity-50`}
            >
              {profile.following ? <Minus size={14} /> : <Plus size={14} />}
              {profile.following ? 'Unfollow' : 'Follow'} {profile.username}
            </button>
          ) : null}
        </div>
      </div>

      <div className="max-w-6xl mx-auto px-4 py-8">
        <ul className="flex border-b border-gray-200 mb-4">
          <li>
            <button
              onClick={() => handleTabChange('my')}
              className={`px-4 py-2 border-b-2 -mb-px ${
                activeTab === 'my'
                  ? 'text-green-500 border-green-500'
                  : 'text-gray-500 border-transparent hover:text-gray-700'
              }`}
            >
              My Articles
            </button>
          </li>
          <li>
            <button
              onClick={() => handleTabChange('favorited')}
              className={`px-4 py-2 border-b-2 -mb-px ${
                activeTab === 'favorited'
                  ? 'text-green-500 border-green-500'
                  : 'text-gray-500 border-transparent hover:text-gray-700'
              }`}
            >
              Favorited Articles
            </button>
          </li>
        </ul>

        <ArticleList
          articles={currentData.data?.articles}
          isLoading={currentData.isLoading}
        />
        <Pagination
          currentPage={currentPage}
          totalPages={totalPages}
          onPageChange={setCurrentPage}
        />
      </div>
    </div>
  );
}
