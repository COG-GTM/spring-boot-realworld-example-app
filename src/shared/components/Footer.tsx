import { Link } from 'react-router-dom';

export function Footer() {
  return (
    <footer className="bg-gray-100 py-4 mt-auto">
      <div className="max-w-6xl mx-auto px-4">
        <div className="flex items-center justify-center gap-2 text-sm text-gray-600">
          <Link to="/" className="text-green-500 font-bold">
            conduit
          </Link>
          <span>
            An interactive learning project from{' '}
            <a
              href="https://thinkster.io"
              target="_blank"
              rel="noopener noreferrer"
              className="text-green-500 hover:underline"
            >
              Thinkster
            </a>
            . Code &amp; design licensed under MIT.
          </span>
        </div>
      </div>
    </footer>
  );
}
