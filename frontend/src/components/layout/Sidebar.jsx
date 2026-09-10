import React from 'react';
import { NavLink } from 'react-router-dom';
import { LayoutDashboard, Users, UserCheck, FileText, Building2, Bike, CreditCard, FolderOpen, BarChart3, Settings, LogOut } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';

export const Sidebar = ({ navItems }) => {
  const { logout } = useAuth();
  
  return (
    <aside className="w-64 bg-primary-900 text-white min-h-screen flex flex-col">
      <div className="p-6 flex items-center justify-center border-b border-primary-700">
        <h2 className="text-2xl font-bold text-accent-500">Auto Consult</h2>
      </div>
      <nav className="flex-1 px-4 py-6 space-y-2">
        {navItems.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            className={({ isActive }) =>
              `flex items-center gap-3 px-4 py-3 rounded-lg transition-all duration-200 ${
                isActive ? 'bg-primary-700 text-accent-500' : 'hover:bg-primary-800 text-gray-300'
              }`
            }
          >
            {item.icon}
            <span>{item.name}</span>
          </NavLink>
        ))}
      </nav>
      <div className="p-4 border-t border-primary-700">
        <button
          onClick={logout}
          className="flex items-center gap-3 px-4 py-3 w-full text-left text-red-400 hover:bg-primary-800 rounded-lg transition-all duration-200"
        >
          <LogOut size={20} />
          <span>Logout</span>
        </button>
      </div>
    </aside>
  );
};
export default Sidebar;
