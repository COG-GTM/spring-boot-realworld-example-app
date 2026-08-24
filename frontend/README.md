# RealWorld React frontend

This optional React client consumes the repository's Spring Boot REST API.

## Implemented flows

- registration, login, logout, token persistence, and current-user loading
- global and personalized article feeds
- tag filtering
- article creation, editing, deletion, favoriting, and unfavoriting
- profiles, following, and favorited-article lists
- comment listing, creation, and deletion
- user settings

## Stack

- React 18.2 and React Router 6.26
- TypeScript 5.2
- Vite 5.2
- Tailwind CSS 3.4
- Axios 1.7

## Prerequisites

- Node.js 18+ and npm
- The Spring Boot backend running at `http://localhost:8080`

## Install and run

```bash
cp .env.example .env
npm ci
npm run dev
```

The Vite development server listens on `http://localhost:3000`.

`VITE_API_BASE_URL` controls the backend URL:

```dotenv
VITE_API_BASE_URL=http://localhost:8080
```

The Axios request interceptor reads the JWT from local storage and sends:

```text
Authorization: Token <jwt>
```

## Commands

```bash
npm run dev      # development server on port 3000
npm run lint     # ESLint
npm run build    # TypeScript check and production bundle
npm run preview  # preview the production bundle
```

`npm run lint` currently stops because the project has no ESLint configuration. `npm run build` currently stops on unused React imports reported by the TypeScript compiler.

## Structure

```text
frontend/
├── src/
│   ├── components/     # Header, article, comment, and tag UI
│   ├── hooks/          # Authentication context and local token state
│   ├── pages/          # Route-level screens
│   ├── services/       # REST client functions
│   ├── types/          # Shared API types
│   ├── App.tsx         # Routes and protected-route wrapper
│   └── main.tsx        # React entry point
├── .env.example
├── package.json
├── tailwind.config.js
└── vite.config.ts
```

The backend uses root-level REST paths such as `/articles` and `/users`; there is no `/api` prefix.
