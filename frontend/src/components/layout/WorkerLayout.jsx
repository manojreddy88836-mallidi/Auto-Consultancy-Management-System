import React, { useState } from 'react';
import { Outlet, NavLink, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard, ClipboardList, FileCheck, LogOut,
  Bike, Menu, Tag, Wrench, CreditCard, Users, ShieldAlert,
  ChevronDown, ChevronRight
} from 'lucide-react';
import useAuth from '../../hooks/useAuth';

const NAV_GROUPS = [
  {
    label: 'Field Work',
    items: [
      { name: 'Dashboard',            path: '/worker',              icon: LayoutDashboard, end: true  },
      { name: 'All My Tasks',         path: '/worker/tasks',        icon: ClipboardList,   end: false },
      { name: 'Repair / Service',     path: '/worker/service',      icon: Wrench,          end: false },
      { name: 'Payment Collections',  path: '/worker/collections',  icon: CreditCard,      end: false },
      { name: 'Customer Visits',      path: '/worker/visits',       icon: Users,           end: false },
      { name: 'Bike Recovery',        path: '/worker/recovery',     icon: ShieldAlert,     end: false },
    ]
  },
  {
    label: 'Applications',
    items: [
      { name: 'Assigned Applications', path: '/worker/applications', icon: ClipboardList, end: false },
      { name: 'Document Verification', path: '/worker/documents',    icon: FileCheck,     end: false },
      { name: 'Sale Inventory',        path: '/worker/bikes',        icon: Bike,          end: false },
      { name: 'Price Offers',          path: '/worker/offers',       icon: Tag,           end: false },
    ]
  }
];

// ?? Sidebar is defined OUTSIDE WorkerLayout to avoid re-creation on every render ??
const SidebarContent = ({ initials, displayName, email, collapsed, toggleGroup, onNavClick, onLogout }) => (
  <div className="flex flex-col h-full bg-[#0F1B35] text-white w-64 flex-shrink-0">
    {/* Logo */}
    <div className="flex items-center gap-3 px-6 py-5 border-b border-white/10">
      <div className="w-8 h-8 bg-[#1E88E5] rounded-lg flex items-center justify-center flex-shrink-0">
        <Bike size={18} className="text-white"/>
      </div>
      <div>
        <h2 className="text-base font-bold leading-tight">Auto Consultancy</h2>
        <p className="text-blue-300 text-xs">Worker Panel</p>
      </div>
    </div>

    {/* Nav */}
    <nav className="flex-1 px-3 py-4 space-y-4 overflow-y-auto">
      {NAV_GROUPS.map(group => (
        <div key={group.label}>
          <button
            onClick={() => toggleGroup(group.label)}
            className="flex items-center justify-between w-full px-3 py-1 mb-1 text-xs font-semibold text-gray-500 uppercase tracking-wider hover:text-gray-300 transition-colors"
          >
            {group.label}
            {collapsed[group.label] ? <ChevronRight size={12}/> : <ChevronDown size={12}/>}
          </button>
          {!collapsed[group.label] && (
            <div className="space-y-0.5">
              {group.items.map(({ name, path, icon: Icon, end }) => (
                <NavLink key={path} to={path} end={end}
                  onClick={onNavClick}
                  className={({ isActive }) =>
                    `flex items-center gap-3 px-4 py-2.5 rounded-xl transition-all duration-200 group ${
                      isActive
                        ? 'bg-[#1E88E5] text-white shadow-md shadow-blue-900/40'
                        : 'text-gray-400 hover:bg-white/8 hover:text-white'
                    }`
                  }>
                  {({ isActive }) => (
                    <>
                      <Icon size={17} className={isActive ? 'text-white' : 'text-gray-400 group-hover:text-white'}/>
                      <span className="text-sm font-medium">{name}</span>
                    </>
                  )}
                </NavLink>
              ))}
            </div>
          )}
        </div>
      ))}
    </nav>

    {/* User footer */}
    <div className="border-t border-white/10 p-3 space-y-1">
      <div className="flex items-center gap-3 px-3 py-2.5 rounded-xl bg-white/5 mb-1">
        <div className="w-8 h-8 rounded-full bg-[#1E88E5] flex items-center justify-center text-xs font-bold flex-shrink-0">{initials}</div>
        <div className="flex-1 min-w-0">
          <p className="text-sm font-medium text-white truncate">{displayName}</p>
          <p className="text-xs text-gray-400 truncate">{email}</p>
        </div>
      </div>
      <button onClick={onLogout}
        className="flex items-center gap-3 px-4 py-2.5 w-full rounded-xl text-red-400 hover:bg-red-500/10 hover:text-red-300 transition-colors">
        <LogOut size={18}/> <span className="text-sm font-medium">Logout</span>
      </button>
    </div>
  </div>
);

const WorkerLayout = () => {
  const { logout, user } = useAuth();
  const navigate = useNavigate();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [collapsed, setCollapsed] = useState({});

  const handleLogout = () => { logout(); navigate('/login'); };
  const displayName = `${user?.firstName || ''} ${user?.lastName || ''}`.trim() || 'Worker';
  const initials    = `${user?.firstName?.[0] || ''}${user?.lastName?.[0] || ''}`.toUpperCase() || 'W';
  const toggleGroup = (label) => setCollapsed(c => ({ ...c, [label]: !c[label] }));
  const closeNav    = () => setMobileOpen(false);

  const sidebarProps = {
    initials, displayName, email: user?.email,
    collapsed, toggleGroup,
    onNavClick: closeNav,
    onLogout: handleLogout
  };

  return (
    <div className="flex h-screen overflow-hidden bg-[#F8FAFC] font-sans">
      {/* Desktop sidebar */}
      <div className="hidden lg:flex flex-shrink-0">
        <SidebarContent {...sidebarProps}/>
      </div>

      {/* Mobile sidebar overlay */}
      {mobileOpen && (
        <div className="lg:hidden fixed inset-0 z-50 flex">
          <SidebarContent {...sidebarProps}/>
          <div className="flex-1 bg-black/50" onClick={closeNav}/>
        </div>
      )}

      {/* Main content */}
      <div className="flex-1 flex flex-col overflow-hidden">
        <header className="bg-white border-b border-gray-100 px-6 py-3.5 flex items-center justify-between shadow-sm flex-shrink-0">
          <div className="flex items-center gap-3">
            <button onClick={() => setMobileOpen(true)} className="lg:hidden p-2 hover:bg-gray-100 rounded-lg">
              <Menu size={20} className="text-gray-600"/>
            </button>
            <span className="text-sm font-semibold text-gray-500">Worker Portal</span>
          </div>
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-full bg-[#1E88E5] flex items-center justify-center text-xs font-bold text-white">{initials}</div>
            <div className="hidden sm:block">
              <p className="text-sm font-semibold text-gray-800">{displayName}</p>
              <p className="text-xs text-gray-400">Worker</p>
            </div>
          </div>
        </header>
        <main className="flex-1 overflow-y-auto p-6"><Outlet/></main>
      </div>
    </div>
  );
};

export default WorkerLayout;