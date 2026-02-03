import { Link, NavLink } from 'react-router-dom';
import { useAuthStore } from '@/features/auth/store/authStore';
import { PenSquare, Settings, User } from 'lucide-react';

export function Header() {
  const { isAuthenticated, user } = useAuthStore();

  return (
    <nav className="bg-white border-b border-gray-200">
      <div className="max-w-6xl mx-auto px-4">
        <div className="flex justify-between items-center h-14">
          <Link to="/" className="text-2xl font-bold text-green-500">
            conduit
          </Link>

          <ul className="flex items-center space-x-4">
            <li>
              <NavLink
                to="/"
                className={({ isActive }) =>
                  `text-gray-600 hover:text-gray-900 ${isActive ? 'text-gray-900' : ''}`
                }
              >
                Home
              </NavLink>
            </li>

            {isAuthenticated ? (
              <>
                <li>
                  <NavLink
                    to="/editor"
                    className={({ isActive }) =>
                      `flex items-center gap-1 text-gray-600 hover:text-gray-900 ${isActive ? 'text-gray-900' : ''}`
                    }
                  >
                    <PenSquare size={16} />
                    New Article
                  </NavLink>
                </li>
                <li>
                  <NavLink
                    to="/settings"
                    className={({ isActive }) =>
                      `flex items-center gap-1 text-gray-600 hover:text-gray-900 ${isActive ? 'text-gray-900' : ''}`
                    }
                  >
                    <Settings size={16} />
                    Settings
                  </NavLink>
                </li>
                <li>
                  <NavLink
                    to={`/profile/${user?.username}`}
                    className={({ isActive }) =>
                      `flex items-center gap-1 text-gray-600 hover:text-gray-900 ${isActive ? 'text-gray-900' : ''}`
                    }
                  >
                    {user?.image ? (
                      <img
                        src={user.image}
                        alt={user.username}
                        className="w-6 h-6 rounded-full"
                      />
                    ) : (
                      <User size={16} />
                    )}
                    {user?.username}
                  </NavLink>
                </li>
              </>
            ) : (
              <>
                <li>
                  <NavLink
                    to="/login"
                    className={({ isActive }) =>
                      `text-gray-600 hover:text-gray-900 ${isActive ? 'text-gray-900' : ''}`
                    }
                  >
                    Sign in
                  </NavLink>
                </li>
                <li>
                  <NavLink
                    to="/register"
                    className={({ isActive }) =>
                      `text-gray-600 hover:text-gray-900 ${isActive ? 'text-gray-900' : ''}`
                    }
                  >
                    Sign up
                  </NavLink>
                </li>
              </>
            )}
          </ul>
        </div>
      </div>
    </nav>
  );
}
