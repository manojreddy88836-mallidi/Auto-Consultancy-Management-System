import React, { useState, useEffect } from 'react';
import { Link, NavLink, Outlet } from 'react-router-dom';
import { Menu, X } from 'lucide-react';

export const PublicLayout = ({ children }) => {
  const [isScrolled, setIsScrolled] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 20);
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  const navLinks = [
    { name: 'Home', path: '/' },
    { name: 'About', path: '/about' },
    { name: 'Services', path: '/services' },
    { name: 'EMI Calculator', path: '/emi-calculator' },
    { name: 'Contact', path: '/contact' },
  ];

  return (
    <div className="min-h-screen flex flex-col font-sans">
      {/* Navbar */}
      <header 
        className={`fixed w-full top-0 z-50 transition-all duration-300 ${
          isScrolled ? 'bg-white shadow-md py-3' : 'bg-primary-900/90 backdrop-blur-md py-5 text-white'
        }`}
      >
        <div className="container mx-auto px-6 flex justify-between items-center">
          <Link to="/" className="text-2xl font-bold flex items-center gap-2">
            <span className={isScrolled ? 'text-primary-900' : 'text-white'}>Auto</span>
            <span className="text-accent-500">Consult</span>
          </Link>

          {/* Desktop Nav */}
          <nav className="hidden md:flex items-center space-x-8">
            {navLinks.map(link => (
              <NavLink 
                key={link.path} 
                to={link.path}
                className={({ isActive }) => `text-sm font-medium transition-colors ${
                  isActive ? 'text-accent-500' : isScrolled ? 'text-gray-600 hover:text-primary-900' : 'text-gray-300 hover:text-white'
                }`}
              >
                {link.name}
              </NavLink>
            ))}
          </nav>

          <div className="hidden md:flex items-center space-x-4">
            <Link 
              to="/login" 
              className={`text-sm font-medium px-4 py-2 rounded-lg transition-colors ${
                isScrolled ? 'text-gray-600 hover:bg-gray-100' : 'text-gray-300 hover:bg-white/10'
              }`}
            >
              Login
            </Link>
            <Link 
              to="/register" 
              className="bg-accent-500 hover:bg-accent-600 text-white text-sm font-medium px-5 py-2.5 rounded-lg transition-colors shadow-sm hover:shadow-md"
            >
              Register
            </Link>
          </div>

          {/* Mobile Menu Button */}
          <button 
            className="md:hidden"
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
          >
            {mobileMenuOpen ? (
              <X className={isScrolled ? 'text-gray-900' : 'text-white'} />
            ) : (
              <Menu className={isScrolled ? 'text-gray-900' : 'text-white'} />
            )}
          </button>
        </div>

        {/* Mobile Menu Dropdown */}
        {mobileMenuOpen && (
          <div className="md:hidden absolute top-full left-0 w-full bg-white shadow-xl py-4 px-6 flex flex-col space-y-4 border-t border-gray-100">
            {navLinks.map(link => (
              <NavLink 
                key={link.path} 
                to={link.path}
                className={({ isActive }) => `text-base font-medium ${isActive ? 'text-accent-500' : 'text-gray-800'}`}
                onClick={() => setMobileMenuOpen(false)}
              >
                {link.name}
              </NavLink>
            ))}
            <div className="pt-4 border-t border-gray-100 flex flex-col space-y-3">
              <Link to="/login" className="text-center text-gray-800 font-medium py-2" onClick={() => setMobileMenuOpen(false)}>Login</Link>
              <Link to="/register" className="text-center bg-accent-500 text-white font-medium py-3 rounded-lg" onClick={() => setMobileMenuOpen(false)}>Register</Link>
            </div>
          </div>
        )}
      </header>

      {/* Main Content */}
      <main className="flex-grow pt-20">
        {children || <Outlet />}
      </main>

      {/* Footer */}
      <footer className="bg-primary-900 text-white pt-16 pb-8">
        <div className="container mx-auto px-6 grid grid-cols-1 md:grid-cols-4 gap-10">
          <div>
            <div className="text-2xl font-bold mb-4">Auto<span className="text-accent-500">Consult</span></div>
            <p className="text-gray-400 text-sm leading-relaxed">
              Your trusted partner for bike finance, document verification, and comprehensive auto consultancy services.
            </p>
          </div>
          <div>
            <h4 className="text-lg font-semibold mb-4">Quick Links</h4>
            <ul className="space-y-2 text-sm text-gray-400">
              <li><Link to="/" className="hover:text-accent-400">Home</Link></li>
              <li><Link to="/about" className="hover:text-accent-400">About Us</Link></li>
              <li><Link to="/services" className="hover:text-accent-400">Services</Link></li>
              <li><Link to="/contact" className="hover:text-accent-400">Contact</Link></li>
            </ul>
          </div>
          <div>
            <h4 className="text-lg font-semibold mb-4">Services</h4>
            <ul className="space-y-2 text-sm text-gray-400">
              <li>Bike Finance</li>
              <li>Document Assistance</li>
              <li>Registration Transfer</li>
              <li>Loan NOC</li>
            </ul>
          </div>
          <div>
            <h4 className="text-lg font-semibold mb-4">Contact</h4>
            <ul className="space-y-2 text-sm text-gray-400">
              <li>info@autoconsult.com</li>
              <li>+91 98765 43210</li>
              <li>123 Auto Avenue, Finance City</li>
            </ul>
          </div>
        </div>
        <div className="container mx-auto px-6 mt-12 pt-8 border-t border-primary-800 text-center text-sm text-gray-500">
          &copy; {new Date().getFullYear()} Auto Consult. All rights reserved.
        </div>
      </footer>
    </div>
  );
};
export default PublicLayout;
