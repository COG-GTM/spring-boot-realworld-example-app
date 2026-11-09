# RealWorld Frontend

A modern React frontend application that consumes the Spring Boot RealWorld API.

## Features

- **User Authentication** - Registration, login, JWT token management
- **Article Management** - Create, view, edit, delete articles (the body is shown as plain text with preserved line breaks; Markdown is not rendered)
- **Article Feeds** - "Global Feed" of all articles and, when logged in, "Your Feed" of followed authors
- **User Profiles** - User information and article listings
- **Comments System** - Add and view comments on articles
- **Social Features** - Following users, favoriting articles
- **Tag System** - Article categorization and filtering
- **Responsive Design** - Modern UI built with Tailwind CSS

## Technology Stack

- **React 18** with TypeScript for type safety
- **Vite** for fast development and building
- **Tailwind CSS** for modern, responsive styling
- **React Router** for navigation
- **Axios** for API communication with JWT authentication

## Getting Started

### Prerequisites

- Node.js 18+ and npm (required by Vite 5)
- Spring Boot backend running on http://localhost:8080

### Installation

```bash
cd frontend
npm install
```

### Development

```bash
npm run dev
```

The application will be available at http://localhost:3000 (configured in `vite.config.ts`).

### Build for Production

```bash
npm run build
```

Note: `npm run build` runs `tsc` first, which currently fails on unused `React` imports (TS6133) in several components, and `npm run lint` fails because the project has no ESLint config file. `npm run dev` works.

## API Integration

The frontend calls the Spring Boot RealWorld REST API (`src/services/api.ts`). The base URL defaults to `http://localhost:8080` and can be overridden with the `VITE_API_BASE_URL` environment variable (e.g. in `frontend/.env.local`).

- **Authentication**: POST /users/login, POST /users, GET/PUT /user
- **Articles**: GET /articles, GET /articles/feed, POST /articles, GET/PUT/DELETE /articles/{slug}
- **Favorites**: POST/DELETE /articles/{slug}/favorite
- **Profiles**: GET /profiles/{username}, POST/DELETE /profiles/{username}/follow
- **Comments**: GET/POST /articles/{slug}/comments, DELETE /articles/{slug}/comments/{id}
- **Tags**: GET /tags

See [`../docs/API.md`](../docs/API.md) for the full API reference.

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
