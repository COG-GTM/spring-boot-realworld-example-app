export const API_ENDPOINTS = {
  LOGIN: '/users/login',
  REGISTER: '/users',
  CURRENT_USER: '/user',
  
  PROFILE: (username: string) => `/profiles/${username}`,
  FOLLOW: (username: string) => `/profiles/${username}/follow`,
  
  ARTICLES: '/articles',
  ARTICLES_FEED: '/articles/feed',
  ARTICLE: (slug: string) => `/articles/${slug}`,
  FAVORITE: (slug: string) => `/articles/${slug}/favorite`,
  
  COMMENTS: (slug: string) => `/articles/${slug}/comments`,
  COMMENT: (slug: string, id: number) => `/articles/${slug}/comments/${id}`,
  
  TAGS: '/tags',
} as const;
