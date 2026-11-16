# RealWorld Frontend

A modern React frontend application that consumes the Spring Boot RealWorld API.

## Features

- **User Authentication** - Registration, login, JWT token management
- **Article Management** - Create, view, edit and delete articles (the body is shown as plain text; markdown is not rendered)
- **Article Feeds** - Global feed (optionally filtered by tag) and "Your Feed" for followed users; shows the first 20 articles, with no pagination controls
- **User Profiles** - User information, authored and favorited articles, follow/unfollow
- **Comments System** - Add, view and delete comments on articles
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

The application will be available at http://localhost:3000

### Build for Production

```bash
npm run build
```

Note: this currently fails at the `tsc` step with `TS6133: 'React' is declared but its value is never read` in several components. `npm run lint` also fails because the project has no ESLint config file. `npm run dev` works.

### Configuration

The API base URL comes from `VITE_API_BASE_URL` (see `.env.example`; `.env` sets it to `http://localhost:8080`).

## API Integration

The frontend integrates with the Spring Boot RealWorld API running on localhost:8080:

- **Authentication**: POST /users/login, POST /users, GET/PUT /user
- **Articles**: GET /articles, GET /articles/feed, POST /articles, GET/PUT/DELETE /articles/{slug}, POST/DELETE /articles/{slug}/favorite
- **Profiles**: GET /profiles/{username}, POST/DELETE /profiles/{username}/follow
- **Comments**: GET/POST/DELETE /articles/{slug}/comments
- **Tags**: GET /tags

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
