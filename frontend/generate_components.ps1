$ErrorActionPreference = "Stop"
$baseDir = "c:\Users\LENOVO\OneDrive\Desktop\antigravity\Auto_Consultancy\frontend"

function Write-File {
    param([string]$path, [string]$content)
    $fullPath = Join-Path $baseDir $path
    $dir = Split-Path $fullPath
    if (-not (Test-Path $dir)) {
        New-Item -ItemType Directory -Force -Path $dir | Out-Null
    }
    Set-Content -Path $fullPath -Value $content -Encoding UTF8
}

Write-Host "Generating components..."

Write-File "src\components\layout\Sidebar.jsx" @"
import React from 'react';
import { NavLink } from 'react-router-dom';
import { LayoutDashboard, Users, UserCheck, FileText, Building2, Bike, CreditCard, FolderOpen, BarChart3, Settings, LogOut } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';

export const Sidebar = ({ navItems }) => {
  const { logout } = useAuth();
  
  return (
    <aside className=`"w-64 bg-primary-900 text-white min-h-screen flex flex-col`">
      <div className=`"p-6 flex items-center justify-center border-b border-primary-700`">
        <h2 className=`"text-2xl font-bold text-accent-500`">Auto Consult</h2>
      </div>
      <nav className=`"flex-1 px-4 py-6 space-y-2`">
        {navItems.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            className={({ isActive }) =>
              `"flex items-center gap-3 px-4 py-3 rounded-lg transition-all duration-200 ${
                isActive ? 'bg-primary-700 text-accent-500' : 'hover:bg-primary-800 text-gray-300'
              }`"
            }
          >
            {item.icon}
            <span>{item.name}</span>
          </NavLink>
        ))}
      </nav>
      <div className=`"p-4 border-t border-primary-700`">
        <button
          onClick={logout}
          className=`"flex items-center gap-3 px-4 py-3 w-full text-left text-red-400 hover:bg-primary-800 rounded-lg transition-all duration-200`"
        >
          <LogOut size={20} />
          <span>Logout</span>
        </button>
      </div>
    </aside>
  );
};
export default Sidebar;
"@

Write-File "src\components\layout\Topbar.jsx" @"
import React from 'react';
import { Bell, User } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';

export const Topbar = () => {
  const { user } = useAuth();
  
  return (
    <header className=`"h-16 bg-white border-b border-slate-200 flex items-center justify-between px-6`">
      <div></div>
      <div className=`"flex items-center gap-4`">
        <button className=`"text-slate-500 hover:text-slate-700 relative`">
          <Bell size={20} />
          <span className=`"absolute top-0 right-0 w-2 h-2 bg-red-500 rounded-full`"></span>
        </button>
        <div className=`"flex items-center gap-2 text-slate-700`">
          <div className=`"w-8 h-8 rounded-full bg-primary-100 flex items-center justify-center text-primary-700 font-semibold`">
            <User size={16} />
          </div>
          <span className=`"font-medium`">{user?.name || 'User'}</span>
        </div>
      </div>
    </header>
  );
};
export default Topbar;
"@

Write-File "src\components\layout\AdminLayout.jsx" @"
import React from 'react';
import Sidebar from './Sidebar';
import Topbar from './Topbar';
import { LayoutDashboard, Users, UserCheck, FileText, Building2, Bike, CreditCard, FolderOpen, BarChart3, Settings } from 'lucide-react';

const adminNavItems = [
  { name: 'Dashboard', path: '/admin', icon: <LayoutDashboard size={20} /> },
  { name: 'Customers', path: '/admin/customers', icon: <Users size={20} /> },
  { name: 'Workers', path: '/admin/workers', icon: <UserCheck size={20} /> },
  { name: 'Applications', path: '/admin/applications', icon: <FileText size={20} /> },
  { name: 'Manufacturers', path: '/admin/manufacturers', icon: <Building2 size={20} /> },
  { name: 'Bike Models', path: '/admin/bike-models', icon: <Bike size={20} /> },
  { name: 'Finance Records', path: '/admin/finance-records', icon: <CreditCard size={20} /> },
  { name: 'Documents', path: '/admin/documents', icon: <FolderOpen size={20} /> },
  { name: 'Reports', path: '/admin/reports', icon: <BarChart3 size={20} /> }
];

export const AdminLayout = ({ children }) => {
  return (
    <div className=`"flex h-screen overflow-hidden bg-slate-50`">
      <Sidebar navItems={adminNavItems} />
      <div className=`"flex-1 flex flex-col overflow-hidden`">
        <Topbar />
        <main className=`"flex-1 overflow-x-hidden overflow-y-auto bg-slate-50 p-6`">
          {children}
        </main>
      </div>
    </div>
  );
};
export default AdminLayout;
"@

Write-File "src\components\common\Badge.jsx" @"
import React from 'react';
import { APPLICATION_STATUSES } from '../../utils/constants';

const colors = {
  gray: 'bg-gray-100 text-gray-800 border-gray-200',
  blue: 'bg-blue-100 text-blue-800 border-blue-200',
  yellow: 'bg-yellow-100 text-yellow-800 border-yellow-200',
  orange: 'bg-orange-100 text-orange-800 border-orange-200',
  purple: 'bg-purple-100 text-purple-800 border-purple-200',
  indigo: 'bg-indigo-100 text-indigo-800 border-indigo-200',
  green: 'bg-green-100 text-green-800 border-green-200',
  red: 'bg-red-100 text-red-800 border-red-200',
  teal: 'bg-teal-100 text-teal-800 border-teal-200',
};

export const Badge = ({ status }) => {
  const statusObj = APPLICATION_STATUSES.find(s => s.value === status) || { label: status, color: 'gray' };
  const colorClass = colors[statusObj.color] || colors.gray;

  return (
    <span className={`"inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium border ${colorClass}`"}>
      {statusObj.label}
    </span>
  );
};
export default Badge;
"@

Write-File "src\components\common\StatsCard.jsx" @"
import React from 'react';

export const StatsCard = ({ icon, title, value, change, changeType, color }) => {
  return (
    <div className=`"bg-white rounded-xl shadow-sm hover:shadow-md transition-all duration-200 p-6 border border-slate-100`">
      <div className=`"flex items-center justify-between`">
        <div className=`"p-3 rounded-lg bg-`" + color + `"-50 text-`" + color + `"-600`">
          {icon}
        </div>
        {change && (
          <span className={`"text-sm font-medium ${changeType === 'positive' ? 'text-green-600' : 'text-red-600'}`"}>
            {change}
          </span>
        )}
      </div>
      <div className=`"mt-4`">
        <h3 className=`"text-slate-500 text-sm font-medium`">{title}</h3>
        <p className=`"text-2xl font-bold text-slate-800 mt-1`">{value}</p>
      </div>
    </div>
  );
};
export default StatsCard;
"@

Write-File "src\pages\admin\AdminDashboard.jsx" @"
import React from 'react';
import { Users, FileText, Building2, CheckCircle } from 'lucide-react';
import StatsCard from '../../components/common/StatsCard';

export const AdminDashboard = () => {
  return (
    <div className=`"space-y-6`">
      <h1 className=`"text-2xl font-bold text-slate-800`">Dashboard</h1>
      
      <div className=`"grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6`">
        <StatsCard icon={<Users size={24} />} title="Total Customers" value="1,234" change="+12%" changeType="positive" color="blue" />
        <StatsCard icon={<Users size={24} />} title="Total Workers" value="45" change="+2" changeType="positive" color="indigo" />
        <StatsCard icon={<FileText size={24} />} title="Total Applications" value="8,549" change="+24%" changeType="positive" color="primary" />
        <StatsCard icon={<CheckCircle size={24} />} title="Pending Applications" value="142" change="-5%" changeType="negative" color="orange" />
      </div>

      <div className=`"grid grid-cols-1 lg:grid-cols-2 gap-6`">
        <div className=`"bg-white p-6 rounded-xl shadow-sm border border-slate-100`">
          <h2 className=`"text-lg font-semibold text-slate-800 mb-4`">Recent Applications</h2>
          <div className=`"text-slate-500`">Table placeholder (No data found)</div>
        </div>
        <div className=`"bg-white p-6 rounded-xl shadow-sm border border-slate-100`">
          <h2 className=`"text-lg font-semibold text-slate-800 mb-4`">Application Status Overview</h2>
          <div className=`"text-slate-500`">Chart placeholder</div>
        </div>
      </div>
    </div>
  );
};
export default AdminDashboard;
"@

Write-File "src\pages\public\HomePage.jsx" @"
import React from 'react';
import { Link } from 'react-router-dom';

export const HomePage = () => {
  return (
    <div className=`"min-h-screen bg-slate-50`">
      <nav className=`"bg-primary-900 text-white p-4 sticky top-0 z-50 shadow-md`">
        <div className=`"container mx-auto flex justify-between items-center`">
          <h1 className=`"text-2xl font-bold text-accent-500`">Auto Consult</h1>
          <div className=`"space-x-4`">
            <Link to="/login" className=`"text-gray-300 hover:text-white`">Login</Link>
            <Link to="/register" className=`"bg-accent-600 hover:bg-accent-700 px-4 py-2 rounded text-white font-medium`">Register</Link>
          </div>
        </div>
      </nav>
      <main className=`"container mx-auto mt-12 text-center`">
        <h2 className=`"text-5xl font-extrabold text-primary-900 mb-4`">Your Trusted Partner in Bike Finance Consultancy</h2>
        <p className=`"text-xl text-slate-600 mb-8`">We handle your vehicle finance and registration smoothly and quickly.</p>
        <Link to="/register" className=`"bg-accent-600 hover:bg-accent-700 text-white px-8 py-3 rounded-lg text-lg font-semibold transition-all shadow-md`">
          Get Started
        </Link>
      </main>
    </div>
  );
};
export default HomePage;
"@

Write-File "src\pages\auth\LoginPage.jsx" @"
import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';

export const LoginPage = () => {
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = (e) => {
    e.preventDefault();
    login('admin@test.com', 'password').then((user) => {
      if(user.role === 'ADMIN') navigate('/admin');
      else navigate('/');
    });
  };

  return (
    <div className=`"min-h-screen flex items-center justify-center bg-slate-50`">
      <div className=`"bg-white p-8 rounded-xl shadow-md w-full max-w-md border border-slate-100`">
        <h2 className=`"text-2xl font-bold text-center text-primary-900 mb-6`">Login to Auto Consult</h2>
        <form onSubmit={handleSubmit} className=`"space-y-4`">
          <div>
            <label className=`"block text-sm font-medium text-slate-700 mb-1`">Email</label>
            <input type="email" className=`"w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-accent-500 focus:border-accent-500`" required />
          </div>
          <div>
            <label className=`"block text-sm font-medium text-slate-700 mb-1`">Password</label>
            <input type="password" className=`"w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-accent-500 focus:border-accent-500`" required />
          </div>
          <button type="submit" className=`"w-full bg-primary-600 hover:bg-primary-700 text-white py-2 rounded-lg font-medium transition-all`">Login</button>
        </form>
      </div>
    </div>
  );
};
export default LoginPage;
"@

Write-File "src\App.jsx" @"
import React from 'react';
import { Routes, Route, Navigate, Outlet } from 'react-router-dom';
import ProtectedRoute from './routes/ProtectedRoute';

import HomePage from './pages/public/HomePage';
import LoginPage from './pages/auth/LoginPage';
import AdminDashboard from './pages/admin/AdminDashboard';
import AdminLayout from './components/layout/AdminLayout';

const App = () => {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<div>Register</div>} />
      
      <Route element={<ProtectedRoute allowedRoles={['ADMIN']} />}>
        <Route path="/admin" element={<AdminLayout><Outlet /></AdminLayout>}>
          <Route index element={<AdminDashboard />} />
          <Route path="customers" element={<div>Customers Page</div>} />
          <Route path="workers" element={<div>Workers Page</div>} />
          <Route path="applications" element={<div>Applications Page</div>} />
          <Route path="manufacturers" element={<div>Manufacturers Page</div>} />
          <Route path="bike-models" element={<div>Bike Models Page</div>} />
          <Route path="finance-records" element={<div>Finance Records Page</div>} />
          <Route path="documents" element={<div>Documents Page</div>} />
          <Route path="reports" element={<div>Reports Page</div>} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
};
export default App;
"@

Write-Host "Done!"
