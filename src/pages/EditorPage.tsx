import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useArticle, useCreateArticle, useUpdateArticle } from '@/features/articles/hooks/useArticles';
import { FormErrors } from '@/shared/components/FormErrors';
import axios from 'axios';

export function EditorPage() {
  const { slug } = useParams<{ slug: string }>();
  const navigate = useNavigate();
  const isEditing = !!slug;

  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [body, setBody] = useState('');
  const [tagInput, setTagInput] = useState('');
  const [tagList, setTagList] = useState<string[]>([]);
  const [errors, setErrors] = useState<{ [key: string]: string[] } | undefined>();

  const { data: articleData, isLoading: isLoadingArticle } = useArticle(slug || '');
  const createMutation = useCreateArticle();
  const updateMutation = useUpdateArticle();

  useEffect(() => {
    if (articleData?.article) {
      setTitle(articleData.article.title);
      setDescription(articleData.article.description);
      setBody(articleData.article.body);
      setTagList(articleData.article.tagList);
    }
  }, [articleData]);

  const handleAddTag = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      const tag = tagInput.trim();
      if (tag && !tagList.includes(tag)) {
        setTagList([...tagList, tag]);
        setTagInput('');
      }
    }
  };

  const handleRemoveTag = (tagToRemove: string) => {
    setTagList(tagList.filter((tag) => tag !== tagToRemove));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrors(undefined);

    try {
      if (isEditing && slug) {
        await updateMutation.mutateAsync({ slug, title, description, body });
      } else {
        await createMutation.mutateAsync({ title, description, body, tagList });
      }
    } catch (error) {
      if (axios.isAxiosError(error) && error.response?.data?.errors) {
        setErrors(error.response.data.errors);
      } else {
        setErrors({ error: ['An unexpected error occurred'] });
      }
    }
  };

  if (isEditing && isLoadingArticle) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-8">
        <div className="text-gray-500">Loading article...</div>
      </div>
    );
  }

  const isPending = createMutation.isPending || updateMutation.isPending;

  return (
    <div className="max-w-3xl mx-auto px-4 py-8">
      <FormErrors errors={errors} />

      <form onSubmit={handleSubmit}>
        <div className="mb-4">
          <input
            type="text"
            placeholder="Article Title"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            className="w-full px-4 py-3 text-xl border border-gray-300 rounded focus:outline-none focus:border-green-500"
            required
          />
        </div>
        <div className="mb-4">
          <input
            type="text"
            placeholder="What's this article about?"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            className="w-full px-4 py-3 border border-gray-300 rounded focus:outline-none focus:border-green-500"
            required
          />
        </div>
        <div className="mb-4">
          <textarea
            placeholder="Write your article (in markdown)"
            value={body}
            onChange={(e) => setBody(e.target.value)}
            className="w-full px-4 py-3 border border-gray-300 rounded focus:outline-none focus:border-green-500 resize-none"
            rows={8}
            required
          />
        </div>
        <div className="mb-4">
          <input
            type="text"
            placeholder="Enter tags (press Enter to add)"
            value={tagInput}
            onChange={(e) => setTagInput(e.target.value)}
            onKeyDown={handleAddTag}
            className="w-full px-4 py-3 border border-gray-300 rounded focus:outline-none focus:border-green-500"
          />
          {tagList.length > 0 && (
            <ul className="flex flex-wrap gap-1 mt-2">
              {tagList.map((tag) => (
                <li
                  key={tag}
                  className="flex items-center gap-1 px-2 py-1 text-sm bg-gray-200 rounded"
                >
                  <button
                    type="button"
                    onClick={() => handleRemoveTag(tag)}
                    className="text-gray-500 hover:text-gray-700"
                  >
                    &times;
                  </button>
                  {tag}
                </li>
              ))}
            </ul>
          )}
        </div>
        <div className="flex justify-end gap-2">
          <button
            type="button"
            onClick={() => navigate(-1)}
            className="px-4 py-3 text-gray-600 hover:text-gray-800"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={isPending}
            className="px-6 py-3 bg-green-500 text-white rounded hover:bg-green-600 disabled:opacity-50"
          >
            {isPending ? 'Publishing...' : isEditing ? 'Update Article' : 'Publish Article'}
          </button>
        </div>
      </form>
    </div>
  );
}
