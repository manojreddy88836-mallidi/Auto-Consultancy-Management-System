import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Bike, Home, ArrowLeft, Search } from 'lucide-react';
import useAuth from '../../hooks/useAuth';

const NotFoundPage = () => {
  const navigate  = useNavigate();
  const { user, isAuthenticated } = useAuth();

  const dashboardPath =
    user?.role === 'ADMIN'   ? '/admin'    :
    user?.role === 'WORKER'  ? '/worker'   :
    user?.role === 'CUSTOMER'? '/customer' : '/';

  return (
    <div className="min-h-screen bg-[#F8FAFC] flex flex-col items-center justify-center px-4 font-sans">
      {/* Decorative bg */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute top-1/4 left-1/4 w-64 h-64 bg-[#1E88E5] opacity-5 rounded-full blur-3xl"/>
        <div className="absolute bottom-1/4 right-1/4 w-80 h-80 bg-[#F59E0B] opacity-5 rounded-full blur-3xl"/>
      </div>

      <div className="relative z-10 text-center max-w-lg mx-auto">
        {/* Logo */}
        <div className="flex items-center justify-center gap-2.5 mb-10">
          <div className="w-10 h-10 bg-[#1E88E5] rounded-xl flex items-center justify-center">
            <Bike size={22} className="text-white"/>
          </div>
          <span className="text-lg font-bold text-[#0F1B35]">Auto Consultancy</span>
        </div>

        {/* 404 number */}
        <div className="relative mb-6">
          <p className="text-[10rem] font-black text-gray-100 leading-none select-none">404</p>
          <div className="absolute inset-0 flex items-center justify-center">
            <div className="bg-white rounded-2xl px-8 py-5 shadow-xl border border-gray-100">
              <Search size={32} className="text-[#1E88E5] mx-auto mb-2"/>
              <p className="text-lg font-bold text-gray-900">Page Not Found</p>
            </div>
          </div>
        </div>

        <p className="text-gray-500 text-base mb-8 leading-relaxed">
          The page you're looking for doesn't exist or has been moved.
          Don't worry — let's get you back on track.
        </p>

        {/* Actions */}
        <div className="flex flex-col sm:flex-row items-center justify-center gap-3">
          <button onClick={() => navigate(-1)}
            className="flex items-center gap-2 px-6 py-3 border-2 border-gray-200 text-gray-700 rounded-full font-semibold hover:bg-gray-50 transition-all text-sm">
            <ArrowLeft size={16}/> Go Back
          </button>
          {isAuthenticated ? (
            <Link to={dashboardPath}
              className="flex items-center gap-2 px-6 py-3 bg-[#1E88E5] hover:bg-[#1976D2] text-white rounded-full font-semibold transition-all shadow-md hover:shadow-lg text-sm">
              <Home size={16}/> My Dashboard
            </Link>
          ) : (
            <Link to="/"
              className="flex items-center gap-2 px-6 py-3 bg-[#1E88E5] hover:bg-[#1976D2] text-white rounded-full font-semibold transition-all shadow-md hover:shadow-lg text-sm">
              <Home size={16}/> Back to Home
            </Link>
          )}
        </div>

        {/* Helpful links */}
        <div className="mt-10 pt-8 border-t border-gray-100">
          <p className="text-xs text-gray-400 font-medium uppercase tracking-wider mb-4">Helpful Links</p>
          <div className="flex flex-wrap items-center justify-center gap-x-6 gap-y-2">
            {[
              { label: 'Home', to: '/' },
              { label: 'Services', to: '/services' },
              { label: 'Login', to: '/login' },
              { label: 'Register', to: '/register' },
              { label: 'Contact', to: '/contact' },
            ].map(link => (
              <Link key={link.to} to={link.to}
                className="text-sm text-[#1E88E5] hover:underline font-medium">
                {link.label}
              </Link>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export default NotFoundPage;
