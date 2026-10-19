# RealWorld Frontend

A modern React frontend application that consumes the Spring Boot RealWorld API.

## Features

- **User Authentication** - Registration, login, JWT token management
- **Article Management** - Create, view, edit, delete articles (the body is shown as plain text with preserved line breaks, and Markdown isn't rendered)
- **Article Feeds** - "Global Feed" (all articles, optionally filtered by tag) and "Your Feed" (followed authors, when logged in); each loads the API's default page of 20 articles, and there are no pagination controls
- **User Profiles** - User information and article listings
- **Comments System** - Add and view comments on articles
- **Social Features** - Following users, favoriting articles
- **Tag System** - Article categorization and filtering
- **Responsive Design** - Modern UI built with Tailwind CSS

## Technology Stack

- **React 18** with TypeScript 5
- **Vite 5** (dev server on port 3000)
- **Tailwind CSS 3** via PostCSS + Autoprefixer
- **React Router 6** for navigation
- **Axios** for API calls, with the JWT stored in `localStorage` and sent as `Authorization: Token <jwt>`

## Getting Started

### Prerequisites

- Node.js 18+ and npm (Vite 5 requires Node 18 or newer)
- Spring Boot backend running on http://localhost:8080

### Installation

```bash
cd frontend
npm install
```

### Configuration

The API base URL comes from `VITE_API_BASE_URL` and defaults to `http://localhost:8080`. A `.env` with that value is checked in, and `.env.example` documents it. Put local overrides in `.env.local`, which is git-ignored.

### Development

```bash
npm run dev
```

The application will be available at http://localhost:3000

### Build for Production

```bash
npm run build    # tsc && vite build -> dist/
npm run preview  # serve the production build locally
```

### Known issues

- `npm run build` currently fails at the `tsc` step with `TS6133: 'React' is declared but its value is never read` in `CommentList.tsx`, `Header.tsx`, `TagList.tsx`, `ArticleView.tsx`, `Home.tsx` and `Profile.tsx` (the project uses the automatic JSX runtime and `noUnusedLocals`). `npx vite build` on its own succeeds.
- `npm run lint` fails because the repository has no ESLint configuration file, even though ESLint and its plugins are listed in `devDependencies`.
- `package.json` lists both `tailwindcss@3` (used by `postcss.config.js`) and an unused `@tailwindcss/postcss@4`.

## API Integration

The frontend integrates with the Spring Boot RealWorld API running on localhost:8080:

- **Authentication**: `POST /users/login`, `POST /users`, `GET /user`, `PUT /user`
- **Articles**: `GET /articles`, `GET /articles/feed`, `POST /articles`, `GET/PUT/DELETE /articles/{slug}`
- **Favorites**: `POST/DELETE /articles/{slug}/favorite`
- **Profiles**: `GET /profiles/{username}`, `POST/DELETE /profiles/{username}/follow`
- **Comments**: `GET/POST /articles/{slug}/comments`, `DELETE /articles/{slug}/comments/{id}`
- **Tags**: `GET /tags`

See [`../docs/API.md`](../docs/API.md) for the full backend API reference.

All API calls include proper JWT authentication headers in the format: `Authorization: Token {jwt}`

## Project Structure

```
frontend/
├── src/
│   ├── components/     # Reusable UI components
│   ├── pages/         # Page components (Home, Login, Register, etc.)
│   ├── services/      # API integration layer
│   ├── hooks/         # Authentication context and hooks
│   ├── types/         # TypeScript interfaces
│   └── App.tsx        # Main application component
├── package.json       # Dependencies and scripts
├── vite.config.ts     # Vite configuration
├── tailwind.config.js # Tailwind CSS configuration
└── tsconfig.json      # TypeScript configuration
```
