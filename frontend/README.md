# RealWorld Frontend

A React + TypeScript single-page app for the Spring Boot RealWorld API in this repository.

## Features

These are implemented in `src/pages` and `src/components`:

- **Authentication.** Register, log in and log out. The JWT is kept in `localStorage` and sent as `Authorization: Token <jwt>`.
- **Home.** Global feed, a "Your Feed" tab for logged-in users, and filtering by popular tag (`GET /tags`). Each list loads the first 20 articles; there are no pagination controls yet.
- **Articles.** View, create, edit and delete (authors only). Tags are added in the editor. The body is shown as plain text (`whitespace-pre-wrap`), not rendered Markdown.
- **Comments.** List, add and delete comments on an article.
- **Social.** Favorite or unfavorite articles and follow or unfollow authors.
- **Profiles.** User info with "My Articles" and "Favorited Articles" tabs.
- **Settings.** Update image, username, bio, email and password.

## Technology stack

From [`package.json`](package.json):

- React 18 + TypeScript 5
- Vite 5 (`@vitejs/plugin-react`)
- React Router 6
- Axios
- Tailwind CSS 3 (via PostCSS + Autoprefixer)

## Getting started

### Prerequisites

- Node.js 18+ and npm (required by Vite 5)
- The backend running on http://localhost:8080 (`./gradlew bootRun` from the repository root)

### Install and run

```bash
cd frontend
npm install
npm run dev
```

The dev server listens on http://localhost:3000 (`vite.config.ts` sets `port: 3000` and `host: true`).

### Configuration

The API base URL comes from `VITE_API_BASE_URL` and falls back to `http://localhost:8080` (`src/services/api.ts`). Copy `.env.example` to `.env` (or `.env.local`) to change it:

```bash
VITE_API_BASE_URL=http://localhost:8080
```

### Scripts

| Script | Command | Notes |
| --- | --- | --- |
| `npm run dev` | `vite` | Dev server on port 3000 |
| `npm run build` | `tsc && vite build` | Type-check, then build to `dist/` |
| `npm run preview` | `vite preview` | Serve the production build |
| `npm run lint` | `eslint . --ext ts,tsx ...` | See known issues |

### Known issues

- **`npm run build` fails at the `tsc` step.** Several components import `React` without using it (`noUnusedLocals` is enabled in `tsconfig.json`). `npx vite build` on its own still produces a bundle.
- **`npm run lint` fails.** ESLint is installed but the repository has no ESLint configuration file.
- **CI does not build or check the frontend.**

## Routes

| Path | Page | Login required |
| --- | --- | --- |
| `/` | Home | no |
| `/login`, `/register` | Auth forms | no |
| `/article/:slug` | Article view + comments | no |
| `/profile/:username` | Profile | no |
| `/editor`, `/editor/:slug` | Create / edit article | yes |
| `/settings` | Account settings | yes |

## API usage

`src/services/api.ts` calls these backend REST endpoints (see [`../docs/API.md`](../docs/API.md)):

- **Auth/user:** `POST /users`, `POST /users/login`, `GET /user`, `PUT /user`
- **Articles:** `GET /articles`, `GET /articles/feed`, `GET/POST/PUT/DELETE /articles[/{slug}]`, `POST/DELETE /articles/{slug}/favorite`
- **Comments:** `GET/POST /articles/{slug}/comments`, `DELETE /articles/{slug}/comments/{id}`
- **Profiles:** `GET /profiles/{username}`, `POST/DELETE /profiles/{username}/follow`
- **Tags:** `GET /tags`

## Project structure

```
frontend/
├── src/
│   ├── components/    # ArticleCard, CommentForm, CommentList, Header, TagList
│   ├── pages/         # Home, Login, Register, ArticleView, ArticleEditor, Profile, Settings
│   ├── services/      # api.ts – Axios client + endpoint wrappers
│   ├── hooks/         # useAuth.ts – auth context/provider
│   ├── types/         # Shared TypeScript interfaces
│   ├── App.tsx        # Routes + protected-route wrapper
│   └── main.tsx       # Entry point
├── .env.example       # VITE_API_BASE_URL template
├── vite.config.ts
├── tailwind.config.js / postcss.config.js
└── tsconfig.json
```
