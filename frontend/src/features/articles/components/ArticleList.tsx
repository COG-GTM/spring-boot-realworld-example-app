import { ArticlePreview } from './ArticlePreview';
import type { Article } from '@/shared/types';

interface ArticleListProps {
  articles: Article[] | undefined;
  isLoading: boolean;
}

export function ArticleList({ articles, isLoading }: ArticleListProps) {
  if (isLoading) {
    return <div className="py-6 text-gray-500">Loading articles...</div>;
  }

  if (!articles || articles.length === 0) {
    return <div className="py-6 text-gray-500">No articles are here... yet.</div>;
  }

  return (
    <div>
      {articles.map((article) => (
        <ArticlePreview key={article.slug} article={article} />
      ))}
    </div>
  );
}
