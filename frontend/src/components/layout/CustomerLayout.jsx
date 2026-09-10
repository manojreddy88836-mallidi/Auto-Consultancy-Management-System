import React, { useState, useRef, useEffect } from 'react';
import { Outlet, NavLink, useNavigate, Link } from 'react-router-dom';
import {
  Bike, Menu, X, LogOut, LayoutDashboard, FileText,
  FolderOpen, User, ChevronDown, Plus, Bell, Tag
} from 'lucide-react';
import useAuth from '../../hooks/useAuth';

const NAV = [
  { name: 'Dashboard',      path: '/customer',              end: true,  icon: LayoutDashboard },
  { name: 'Browse Bikes',   path: '/customer/bikes',        end: false, icon: Bike            },
  { name: 'My Applications',path: '/customer/applications', end: false, icon: FileText        },
  { name: 'My Offers',      path: '/customer/offers',       end: false, icon: Tag             },
  { name: 'Documents',      path: '/customer/documents',    end: false, icon: FolderOpen      },
  { name: 'Profile',        path: '/customer/profile',      end: false, icon: User            },
];

const CustomerLayout = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [mobileOpen, setMobileOpen]   = useState(false);
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const dropdownRef = useRef(null);

  const handleLogout = () => { logout(); navigate('/login'); };

  const displayName = `${user?.firstName || ''} ${user?.lastName || ''}`.trim() || 'Customer';
  const initials    = `${user?.firstName?.[0] || ''}${user?.lastName?.[0] || ''}`.toUpperCase() || '?';

  // Close dropdown on outside click
  useEffect(() => {
    const handler = (e) => { if (dropdownRef.current && !dropdownRef.current.contains(e.target)) setDropdownOpen(false); };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  return (
    <div className="min-h-screen bg-[#F8FAFC] font-sans">
      {/* Header */}
      <header className="bg-white border-b border-gray-100 sticky top-0 z-50 shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            {/* Logo */}
            <Link to="/" className="flex items-center gap-2.5 flex-shrink-0">
              <div className="w-8 h-8 bg-[#1E88E5] rounded-lg flex items-center justify-center">
                <Bike size={18} className="text-white"/>
              </div>
              <span className="text-lg font-bold text-[#0F1B35] hidden sm:block">Auto Consultancy</span>
            </Link>

            {/* Desktop nav */}
            <nav className="hidden md:flex items-center gap-1">
              {NAV.map(({ name, path, end, icon: Icon }) => (
                <NavLink key={path} to={path} end={end}
                  className={({ isActive }) =>
                    `flex items-center gap-1.5 px-3.5 py-2 rounded-lg text-sm font-medium transition-colors ${
                      isActive ? 'bg-blue-50 text-[#1E88E5]' : 'text-gray-600 hover:text-[#0F1B35] hover:bg-gray-50'
                    }`
                  }>
                  <Icon size={15}/>
                  {name}
                </NavLink>
              ))}
            </nav>

            {/* Desktop right side */}
            <div className="hidden md:flex items-center gap-3">
              <Link to="/customer/submit"
                className="flex items-center gap-1.5 bg-[#F59E0B] hover:bg-[#D97706] text-white px-4 py-2 rounded-full text-sm font-semibold transition-all shadow-sm hover:shadow-md hover:scale-[1.02]">
                <Plus size={15}/> New Application
              </Link>

              {/* Avatar dropdown */}
              <div className="relative" ref={dropdownRef}>
                <button onClick={() => setDropdownOpen(o => !o)}
                  className="flex items-center gap-2 p-1 rounded-full hover:bg-gray-50 transition-colors focus:outline-none">
                  <div className="w-9 h-9 rounded-full bg-gradient-to-br from-[#0F1B35] to-[#1E88E5] flex items-center justify-center text-white text-sm font-bold">
                    {initials}
                  </div>
                  <ChevronDown size={14} className={`text-gray-400 transition-transform ${dropdownOpen ? 'rotate-180' : ''}`}/>
                </button>

                {dropdownOpen && (
                  <div className="absolute right-0 mt-2 w-56 bg-white rounded-2xl shadow-xl border border-gray-100 py-2 z-50 animate-in slide-in-from-top-2">
                    <div className="px-4 py-3 border-b border-gray-100">
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-full bg-gradient-to-br from-[#0F1B35] to-[#1E88E5] flex items-center justify-center text-white text-sm font-bold flex-shrink-0">
                          {initials}
                        </div>
                        <div className="min-w-0">
                          <p className="text-sm font-bold text-gray-900 truncate">{displayName}</p>
                          <p className="text-xs text-gray-400 truncate">{user?.email}</p>
                        </div>
                      </div>
                    </div>
                    <div className="py-1">
                      <Link to="/customer/profile" onClick={() => setDropdownOpen(false)}
                        className="flex items-center gap-3 px-4 py-2.5 text-sm text-gray-700 hover:bg-gray-50 transition-colors">
                        <User size={15} className="text-gray-400"/> My Profile
                      </Link>
                      <Link to="/customer/applications" onClick={() => setDropdownOpen(false)}
                        className="flex items-center gap-3 px-4 py-2.5 text-sm text-gray-700 hover:bg-gray-50 transition-colors">
                        <FileText size={15} className="text-gray-400"/> My Applications
                      </Link>
                      <Link to="/customer/documents" onClick={() => setDropdownOpen(false)}
                        className="flex items-center gap-3 px-4 py-2.5 text-sm text-gray-700 hover:bg-gray-50 transition-colors">
                        <FolderOpen size={15} className="text-gray-400"/> Documents
                      </Link>
                    </div>
                    <div className="border-t border-gray-100 pt-1">
                      <button onClick={handleLogout}
                        className="flex items-center gap-3 w-full px-4 py-2.5 text-sm text-red-500 hover:bg-red-50 transition-colors">
                        <LogOut size={15}/> Sign Out
                      </button>
                    </div>
                  </div>
                )}
              </div>
            </div>

            {/* Mobile menu button */}
            <button onClick={() => setMobileOpen(o => !o)}
              className="md:hidden p-2 rounded-lg hover:bg-gray-100 transition-colors">
              {mobileOpen ? <X size={22} className="text-gray-600"/> : <Menu size={22} className="text-gray-600"/>}
            </button>
          </div>
        </div>

        {/* Mobile menu */}
        {mobileOpen && (
          <div className="md:hidden border-t border-gray-100 bg-white px-4 pb-4 pt-2">
            <div className="flex items-center gap-3 py-3 mb-2 border-b border-gray-100">
              <div className="w-10 h-10 rounded-full bg-gradient-to-br from-[#0F1B35] to-[#1E88E5] flex items-center justify-center text-white text-sm font-bold">
                {initials}
              </div>
              <div>
                <p className="font-bold text-gray-900 text-sm">{displayName}</p>
                <p className="text-xs text-gray-400">{user?.email}</p>
              </div>
            </div>
            <div className="space-y-0.5">
              {NAV.map(({ name, path, end, icon: Icon }) => (
                <NavLink key={path} to={path} end={end}
                  onClick={() => setMobileOpen(false)}
                  className={({ isActive }) =>
                    `flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-colors ${
                      isActive ? 'bg-blue-50 text-[#1E88E5]' : 'text-gray-700 hover:bg-gray-50'
                    }`
                  }>
                  <Icon size={16}/> {name}
                </NavLink>
              ))}
              <Link to="/customer/submit" onClick={() => setMobileOpen(false)}
                className="flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-semibold text-[#F59E0B] hover:bg-yellow-50 transition-colors">
                <Plus size={16}/> New Application
              </Link>
              <button onClick={handleLogout}
                className="flex items-center gap-3 w-full px-3 py-2.5 rounded-xl text-sm font-medium text-red-500 hover:bg-red-50 transition-colors">
                <LogOut size={16}/> Sign Out
              </button>
            </div>
          </div>
        )}
      </header>

      {/* Page content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <Outlet/>
      </main>
    </div>
  );
};

export default CustomerLayout;
