import React from 'react';
import { Link } from 'react-router-dom';
import type { Comment } from '../types';
import { useAuth } from '../hooks/useAuth';

interface CommentListProps {
  comments: Comment[];
  onDelete: (commentId: string) => Promise<void>;
}

export const CommentList = ({ comments, onDelete }: CommentListProps) => {
  const { user } = useAuth();

  if (comments.length === 0) {
    return (
      <div className="text-center py-8 text-gray-500">
        No comments yet. Be the first to comment!
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {comments.map((comment) => (
        <div key={comment.id} className="border border-gray-200 rounded">
          <div className="p-4">
            <p className="text-gray-800">{comment.body}</p>
          </div>
          <div className="flex justify-between items-center px-4 py-3 bg-gray-50 border-t">
            <div className="flex items-center">
              <Link to={`/profile/${comment.author.username}`}>
                <img
                  src={comment.author.image || 'https://static.productionready.io/images/smiley-cyrus.jpg'}
                  alt={comment.author.username}
                  className="w-6 h-6 rounded-full mr-2"
                />
              </Link>
              <Link
                to={`/profile/${comment.author.username}`}
                className="text-green-500 text-sm hover:underline mr-2"
              >
                {comment.author.username}
              </Link>
              <span className="text-gray-400 text-sm">
                {new Date(comment.createdAt).toLocaleDateString('en-US', {
                  month: 'long',
                  day: 'numeric',
                  year: 'numeric'
                })}
              </span>
            </div>
            
            {user && user.username === comment.author.username && (
              <button
                onClick={() => onDelete(comment.id)}
                className="text-red-500 hover:text-red-700 text-sm"
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                </svg>
              </button>
            )}
          </div>
        </div>
      ))}
    </div>
  );
};
