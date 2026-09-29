import React, { useState } from 'react';
import { NavLink, useNavigate, useLocation } from 'react-router-dom';
import {
  LayoutDashboard, Users, UserCheck, FileText, Building2, Bike,
  CreditCard, FolderOpen, BarChart3, LogOut, ChevronLeft, ChevronRight,
  Menu, Tag, ClipboardList
} from 'lucide-react';
import useAuth from '../../hooks/useAuth';
import { Outlet } from 'react-router-dom';

const adminNavItems = [
  { name: 'Dashboard',       path: '/admin',                  icon: LayoutDashboard, end: true },
  { name: 'Customers',       path: '/admin/customers',        icon: Users,           end: false },
  { name: 'Workers',         path: '/admin/workers',          icon: UserCheck,       end: false },
  { name: 'Applications',    path: '/admin/applications',     icon: FileText,        end: false },
  { name: 'Manufacturers',   path: '/admin/manufacturers',    icon: Building2,       end: false },
  { name: 'Bike Models',     path: '/admin/bike-models',      icon: Bike,            end: false },
  { name: 'Bikes',           path: '/admin/bikes',            icon: Bike,            end: false },
  { name: 'Finance Records', path: '/admin/finance-records',  icon: CreditCard,      end: false },
  { name: 'Documents',       path: '/admin/documents',        icon: FolderOpen,      end: false },
  { name: 'Reports',         path: '/admin/reports',          icon: BarChart3,       end: false },
  { name: 'Offers',          path: '/admin/offers',           icon: Tag,             end: false },
  { name: 'Worker Tasks',    path: '/admin/worker-tasks',     icon: ClipboardList,   end: false },
];

const PAGE_TITLES = {
  '/admin':                    'Dashboard',
  '/admin/customers':          'Customers',
  '/admin/workers':            'Workers',
  '/admin/applications':       'Applications',
  '/admin/manufacturers':      'Manufacturers',
  '/admin/bike-models':        'Bike Models',
  '/admin/bikes':              'Bikes',
  '/admin/manufacturing-years':'Manufacturing Years',
  '/admin/finance-records':    'Finance Records',
  '/admin/documents':          'Documents',
  '/admin/reports':            'Reports & Analytics',
  '/admin/offers':             'Price Offers',
};

// L4 fix: defined at module scope (not inside AdminLayout) so React can properly
// reconcile it on re-renders without unmounting/remounting the sidebar tree.
const SidebarContent = ({ mobile, collapsed, initials, user, handleLogout, onNavClick }) => (
  <div className={`flex flex-col h-full bg-[#0F1B35] text-white ${mobile ? 'w-64' : collapsed ? 'w-16' : 'w-64'} transition-all duration-300`}>
    {/* Logo */}
    <div className={`flex items-center border-b border-white/10 ${collapsed && !mobile ? 'p-4 justify-center' : 'px-6 py-5 gap-3'}`}>
      <div className="w-8 h-8 bg-[#1E88E5] rounded-lg flex items-center justify-center flex-shrink-0">
        <Bike size={18} className="text-white" />
      </div>
      {(!collapsed || mobile) && (
        <div>
          <h2 className="text-base font-bold leading-tight">Auto Consultancy</h2>
          <p className="text-blue-300 text-xs">Admin Panel</p>
        </div>
      )}
    </div>

    {/* Navigation */}
    <nav className={`flex-1 py-4 space-y-0.5 ${collapsed && !mobile ? 'px-2' : 'px-3'} overflow-y-auto`}>
      {adminNavItems.map(({ name, path, icon: Icon, end }) => (
        <NavLink
          key={path}
          to={path}
          end={end}
          onClick={onNavClick}
          className={({ isActive }) =>
            `flex items-center rounded-xl transition-all duration-200 group ${
              collapsed && !mobile ? 'justify-center p-3' : 'gap-3 px-4 py-2.5'
            } ${isActive
              ? 'bg-[#1E88E5] text-white shadow-md shadow-blue-900/40'
              : 'text-gray-400 hover:bg-white/8 hover:text-white'
            }`
          }
        >
          {({ isActive }) => (
            <>
              <Icon size={18} className={isActive ? 'text-white' : 'text-gray-400 group-hover:text-white'} />
              {(!collapsed || mobile) && <span className="text-sm font-medium">{name}</span>}
            </>
          )}
        </NavLink>
      ))}
    </nav>

    {/* User & Logout */}
    <div className="border-t border-white/10 p-3 space-y-1">
      {(!collapsed || mobile) && (
        <div className="flex items-center gap-3 px-3 py-2.5 rounded-xl bg-white/5 mb-1">
          <div className="w-8 h-8 rounded-full bg-[#1E88E5] flex items-center justify-center text-xs font-bold flex-shrink-0">
            {initials}
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-sm font-medium text-white truncate">{user?.firstName} {user?.lastName}</p>
            <p className="text-xs text-gray-400 truncate">{user?.email}</p>
          </div>
        </div>
      )}
      <button
        onClick={handleLogout}
        className={`flex items-center w-full rounded-xl text-red-400 hover:bg-red-500/10 hover:text-red-300 transition-colors duration-200 ${
          collapsed && !mobile ? 'justify-center p-3' : 'gap-3 px-4 py-2.5'
        }`}
      >
        <LogOut size={18} />
        {(!collapsed || mobile) && <span className="text-sm font-medium">Logout</span>}
      </button>
    </div>
  </div>
);

const AdminLayout = () => {
  const { logout, user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [collapsed, setCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);

  const handleLogout = () => { logout(); navigate('/login'); };

  const initials    = `${user?.firstName?.[0] || ''}${user?.lastName?.[0] || ''}`.toUpperCase() || 'A';
  const displayName = `${user?.firstName || ''} ${user?.lastName || ''}`.trim();
  const pageTitle   = Object.entries(PAGE_TITLES).find(([p]) => location.pathname === p || (p !== '/admin' && location.pathname.startsWith(p)))?.[1] || 'Admin';

  return (
    <div className="flex h-screen overflow-hidden bg-[#F8FAFC] font-sans">
      {/* Desktop Sidebar */}
      <div className="hidden lg:flex relative flex-shrink-0">
        <SidebarContent
          collapsed={collapsed}
          initials={initials}
          user={user}
          handleLogout={handleLogout}
          onNavClick={() => setMobileOpen(false)}
        />
        <button
          onClick={() => setCollapsed(!collapsed)}
          className="absolute -right-3 top-8 w-6 h-6 bg-white border border-gray-200 rounded-full flex items-center justify-center shadow-sm hover:shadow-md transition-all z-10"
        >
          {collapsed ? <ChevronRight size={12} className="text-gray-600" /> : <ChevronLeft size={12} className="text-gray-600" />}
        </button>
      </div>

      {/* Mobile Sidebar Overlay */}
      {mobileOpen && (
        <div className="lg:hidden fixed inset-0 z-50 flex">
          <div className="flex-shrink-0">
            <SidebarContent
              mobile
              collapsed={false}
              initials={initials}
              user={user}
              handleLogout={handleLogout}
              onNavClick={() => setMobileOpen(false)}
            />
          </div>
          <div className="flex-1 bg-black/50" onClick={() => setMobileOpen(false)} />
        </div>
      )}

      {/* Main content */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Topbar */}
        <header className="bg-white border-b border-gray-100 px-6 py-3.5 flex items-center justify-between shadow-sm flex-shrink-0">
          <div className="flex items-center gap-3">
            <button onClick={() => setMobileOpen(true)} className="lg:hidden p-2 hover:bg-gray-100 rounded-lg">
              <Menu size={20} className="text-gray-600" />
            </button>
            <div>
              <h1 className="text-sm font-bold text-gray-900">{pageTitle}</h1>
              <p className="text-xs text-gray-400 hidden sm:block">Admin Portal</p>
            </div>
          </div>
          <div className="flex items-center gap-3">
            <div className="flex items-center gap-2.5">
              <div className="w-8 h-8 rounded-full bg-[#1E88E5] flex items-center justify-center text-xs font-bold text-white flex-shrink-0">
                {initials}
              </div>
              <div className="hidden sm:block">
                <p className="text-sm font-semibold text-gray-800">{displayName}</p>
                <p className="text-xs text-gray-400">Administrator</p>
              </div>
            </div>
          </div>
        </header>

        {/* Page content */}
        <main className="flex-1 overflow-y-auto p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
};

export default AdminLayout;
