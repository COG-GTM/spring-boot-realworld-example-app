# RealWorld Frontend

A modern React frontend application that consumes the Spring Boot RealWorld API.

## Features

- **User Authentication** - Registration, login, JWT token management
- **Article Management** - Create, view, edit, delete articles with markdown support
- **Article Feed** - Global feed displaying all articles with pagination
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

- Node.js 16+ and npm
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

## API Integration

The frontend integrates with the Spring Boot RealWorld API running on localhost:8080:

- **Authentication**: POST /users/login, POST /users
- **Articles**: GET/POST/PUT/DELETE /articles
- **Profiles**: GET /profiles/{username}
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
