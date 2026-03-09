import { useTags } from '../hooks/useTags';

interface TagListProps {
  onTagClick: (tag: string) => void;
}

export function TagList({ onTagClick }: TagListProps) {
  const { data, isLoading } = useTags();

  if (isLoading) {
    return <div className="text-gray-500">Loading tags...</div>;
  }

  if (!data?.tags || data.tags.length === 0) {
    return <div className="text-gray-500">No tags available</div>;
  }

  return (
    <div className="flex flex-wrap gap-1">
      {data.tags.map((tag) => (
        <button
          key={tag}
          onClick={() => onTagClick(tag)}
          className="px-2 py-1 text-sm text-white bg-gray-500 rounded-full hover:bg-gray-600"
        >
          {tag}
        </button>
      ))}
    </div>
  );
}
