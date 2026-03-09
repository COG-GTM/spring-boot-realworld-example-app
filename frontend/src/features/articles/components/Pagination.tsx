interface PaginationProps {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}

export function Pagination({ currentPage, totalPages, onPageChange }: PaginationProps) {
  if (totalPages <= 1) return null;

  const pages = Array.from({ length: totalPages }, (_, i) => i + 1);

  return (
    <nav className="mt-4">
      <ul className="flex flex-wrap gap-1">
        {pages.map((page) => (
          <li key={page}>
            <button
              onClick={() => onPageChange(page)}
              className={`px-3 py-1 border rounded ${
                page === currentPage
                  ? 'bg-green-500 text-white border-green-500'
                  : 'text-green-500 border-gray-300 hover:bg-gray-100'
              }`}
            >
              {page}
            </button>
          </li>
        ))}
      </ul>
    </nav>
  );
}
