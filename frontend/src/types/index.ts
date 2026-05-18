export interface User {
  id: string;
  email: string;
  username: string;
  bio: string;
  image: string;
}

export interface UserWithToken {
  email: string;
  username: string;
  bio: string;
  image: string;
  token: string;
}

export interface Profile {
  username: string;
  bio: string;
  image: string;
  following: boolean;
}

export interface Article {
  id: string;
  slug: string;
  title: string;
  description: string;
  body: string;
  favorited: boolean;
  favoritesCount: number;
  createdAt: string;
  updatedAt: string;
  tagList: string[];
  author: Profile;
}

export interface Comment {
  id: string;
  body: string;
  createdAt: string;
  updatedAt: string;
  author: Profile;
}

export interface LoginCredentials {
  email: string;
  password: string;
}

export interface RegisterData {
  username: string;
  email: string;
  password: string;
}

export interface NewArticle {
  title: string;
  description: string;
  body: string;
  tagList: string[];
}

export interface UpdateUser {
  email?: string;
  username?: string;
  bio?: string;
  image?: string;
  password?: string;
}

export interface ArticlesParams {
  tag?: string;
  author?: string;
  favorited?: string;
  limit?: number;
  offset?: number;
}
