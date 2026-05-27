import React from 'react';

interface TagListProps {
  tags: string[];
  selectedTag?: string;
  onTagSelect: (tag: string) => void;
}

export const TagList = ({ tags, selectedTag, onTagSelect }: TagListProps) => {
  if (tags.length === 0) {
    return (
      <div className="bg-gray-100 rounded p-4">
        <h3 className="text-sm font-medium text-gray-900 mb-2">Popular Tags</h3>
        <p className="text-gray-500 text-sm">No tags available</p>
      </div>
    );
  }

  return (
    <div className="bg-gray-100 rounded p-4">
      <h3 className="text-sm font-medium text-gray-900 mb-3">Popular Tags</h3>
      <div className="flex flex-wrap gap-1">
        {tags.map((tag) => (
          <button
            key={tag}
            onClick={() => onTagSelect(tag)}
            className={`px-2 py-1 text-xs rounded-full transition-colors ${
              selectedTag === tag
                ? 'bg-green-500 text-white'
                : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
            }`}
          >
            {tag}
          </button>
        ))}
      </div>
    </div>
  );
};
