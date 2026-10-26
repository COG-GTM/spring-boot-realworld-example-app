# RealWorld Frontend

A React + TypeScript client for the Spring Boot RealWorld REST API in the parent directory.

## Features

- **Authentication** – register, log in, JWT stored in `localStorage`, settings page to update the profile
- **Articles** – create, view, edit and delete articles (the body is shown as plain text; markdown is not rendered)
- **Home** – "Global Feed" (all articles) and, when logged in, "Your Feed" (followed authors); first 20 articles, no pagination controls
- **Profiles** – user info and their articles
- **Comments** – add, list and delete comments
- **Social** – follow users, favorite articles
- **Tags** – popular tags list and tag filtering
- **Styling** – Tailwind CSS

## Technology stack

- React 18 with TypeScript
- Vite 5 (dev server and build)
- Tailwind CSS
- React Router 6
- Axios

## Getting started

### Prerequisites

- Node.js 18+ (required by Vite 5) and npm
- The backend running on http://localhost:8080 (`./gradlew bootRun` from the repository root)

### Install and run

```bash
cd frontend
npm install
npm run dev
```

The app is served at http://localhost:3000 (`vite.config.ts` also binds to all interfaces).

### Backend URL

The API base URL defaults to `http://localhost:8080`. Override it with `VITE_API_BASE_URL`, e.g. in `frontend/.env.local`:

```
VITE_API_BASE_URL=http://localhost:8081
```

### Other scripts

```bash
npm run build     # tsc && vite build (output in dist/)
npm run preview   # serve the production build
npm run lint      # eslint
```

Known issues: `npm run build` currently fails the `tsc` step on unused `React` imports (TS6133) in several components, and `npm run lint` fails because no ESLint config file is checked in. `npm run dev` is unaffected.

## Routes

| Path | Page |
| --- | --- |
| `/` | Home (global feed / your feed, tags) |
| `/login`, `/register` | Auth |
| `/article/:slug` | Article with comments |
| `/profile/:username` | Profile |
| `/editor`, `/editor/:slug` | New / edit article (login required) |
| `/settings` | User settings (login required) |

## API integration

All calls go through `src/services/api.ts` and send `Authorization: Token <jwt>` when logged in:

- **Auth**: `POST /users/login`, `POST /users`, `GET /user`, `PUT /user`
- **Articles**: `GET /articles`, `GET /articles/feed`, `GET/PUT/DELETE /articles/{slug}`, `POST /articles`
- **Favorites**: `POST/DELETE /articles/{slug}/favorite`
- **Comments**: `GET/POST /articles/{slug}/comments`, `DELETE /articles/{slug}/comments/{id}`
- **Profiles**: `GET /profiles/{username}`, `POST/DELETE /profiles/{username}/follow`
- **Tags**: `GET /tags`

## Project structure

```
frontend/
├── src/
│   ├── components/    # ArticleCard, CommentForm, CommentList, Header, TagList
│   ├── pages/         # Home, Login, Register, ArticleView, ArticleEditor, Profile, Settings
│   ├── services/      # api.ts (Axios client)
│   ├── hooks/         # useAuth (auth context)
│   ├── types/         # TypeScript interfaces
│   ├── App.tsx        # routes
│   └── main.tsx       # entry point
├── index.html
├── package.json
├── vite.config.ts
├── tailwind.config.js
├── postcss.config.js
└── tsconfig.json
```
