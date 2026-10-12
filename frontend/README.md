# RealWorld Frontend

A modern React frontend application that consumes the Spring Boot RealWorld API.

## Features

- **User Authentication** - Registration, login, JWT token management
- **Article Management** - Create, view, edit, delete articles (bodies are rendered as plain text; markdown is not rendered)
- **Article Feed** - Global feed and "Your Feed" (followed authors) tabs, filterable by tag; shows the first 20 articles (no pager UI yet)
- **User Profiles** - User information and article listings
- **Comments System** - Add and view comments on articles
- **Social Features** - Following users, favoriting articles
- **Tag System** - Article categorization and filtering
- **Responsive Design** - Modern UI built with Tailwind CSS

## Technology Stack

Versions from [`package.json`](package.json):

- **React 18.2** with **TypeScript 5.2**
- **Vite 5.2** for development and building
- **Tailwind CSS 3.4** for styling
- **React Router 6.26** for navigation
- **Axios 1.7** for API communication with JWT authentication

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

The application will be available at http://localhost:3000 (`vite.config.ts` also binds to all interfaces).

### Configuration

The API base URL comes from `VITE_API_BASE_URL` and defaults to `http://localhost:8080` (see [`src/services/api.ts`](src/services/api.ts)). Copy [`.env.example`](.env.example) to `.env` (or edit the committed `.env`) to point at another backend.

The JWT returned on login/registration is stored in `localStorage` under the key `token` and sent as `Authorization: Token {jwt}`.

### Routes

| Path | Page |
| --- | --- |
| `/` | Home (global feed / your feed, tag filter) |
| `/login`, `/register` | Authentication |
| `/article/:slug` | Article with comments |
| `/profile/:username` | Profile with authored articles |
| `/editor`, `/editor/:slug` | Create / edit article |
| `/settings` | Update current user |

### Build for Production

```bash
npm run build    # tsc && vite build
npm run preview  # serve the production build locally
```

### Known issues

- `npm run build` currently fails at the `tsc` step with `TS6133: 'React' is declared but its value is never read` in several components (`noUnusedLocals` is enabled and the new JSX transform makes the default `React` import unused). `npm run dev` is unaffected because Vite does not type-check.
- `npm run lint` fails because no ESLint configuration file is checked in, even though ESLint plugins are listed in `devDependencies`.

## API Integration

All calls go through [`src/services/api.ts`](src/services/api.ts):

- **Authentication**: `POST /users/login`, `POST /users`, `GET /user`, `PUT /user`
- **Articles**: `GET /articles`, `GET /articles/feed`, `GET/PUT/DELETE /articles/{slug}`, `POST /articles`
- **Favorites**: `POST/DELETE /articles/{slug}/favorite`
- **Profiles**: `GET /profiles/{username}`, `POST/DELETE /profiles/{username}/follow`
- **Comments**: `GET/POST /articles/{slug}/comments`, `DELETE /articles/{slug}/comments/{id}`
- **Tags**: `GET /tags`

The frontend uses only the REST API; the backend's GraphQL API is not used. See [../docs/API.md](../docs/API.md) for the full reference.

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
