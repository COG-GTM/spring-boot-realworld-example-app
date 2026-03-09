import { useAuthStore } from '@/features/auth/store/authStore';

interface FeedTabsProps {
  activeTab: 'global' | 'feed' | 'tag';
  selectedTag?: string;
  onTabChange: (tab: 'global' | 'feed' | 'tag') => void;
}

export function FeedTabs({ activeTab, selectedTag, onTabChange }: FeedTabsProps) {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);

  return (
    <ul className="flex border-b border-gray-200">
      {isAuthenticated && (
        <li>
          <button
            onClick={() => onTabChange('feed')}
            className={`px-4 py-2 border-b-2 -mb-px ${
              activeTab === 'feed'
                ? 'text-green-500 border-green-500'
                : 'text-gray-500 border-transparent hover:text-gray-700'
            }`}
          >
            Your Feed
          </button>
        </li>
      )}
      <li>
        <button
          onClick={() => onTabChange('global')}
          className={`px-4 py-2 border-b-2 -mb-px ${
            activeTab === 'global'
              ? 'text-green-500 border-green-500'
              : 'text-gray-500 border-transparent hover:text-gray-700'
          }`}
        >
          Global Feed
        </button>
      </li>
      {selectedTag && activeTab === 'tag' && (
        <li>
          <button
            className="px-4 py-2 border-b-2 -mb-px text-green-500 border-green-500"
          >
            # {selectedTag}
          </button>
        </li>
      )}
    </ul>
  );
}
