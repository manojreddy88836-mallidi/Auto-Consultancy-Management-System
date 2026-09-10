import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Car, Eye, EyeOff, ShieldCheck } from 'lucide-react';
import { toast } from 'react-hot-toast';
import useAuth from '../../hooks/useAuth';

const LoginPage = () => {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [showPassword, setShowPassword] = useState(false);
  const [formData, setFormData] = useState({ email: '', password: '' });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
    setError('');
  };

  const fillDemo = (role) => {
    if (role === 'admin') setFormData({ email: 'admin@autoconsultancy.com', password: 'Admin@123' });
    else if (role === 'worker') setFormData({ email: 'worker1@autoconsultancy.com', password: 'Worker@123' });
    else setFormData({ email: 'customer@autoconsultancy.com', password: 'Customer@123' });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!formData.email || !formData.password) {
      setError('Please fill in all fields');
      return;
    }
    setLoading(true);
    try {
      const userData = await login(formData.email, formData.password);
      toast.success(`Welcome back, ${userData.firstName}!`);
      // Redirect based on actual role from server
      if (userData.role === 'ADMIN') navigate('/admin');
      else if (userData.role === 'WORKER') navigate('/worker');
      else navigate('/customer');
    } catch (err) {
      const msg = err?.response?.data?.message || 'Invalid email or password';
      setError(msg);
      toast.error('Login failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex font-sans">
      {/* Left Branding Panel */}
      <div className="hidden lg:flex w-[42%] bg-[#0F1B35] text-white flex-col justify-between p-12 relative overflow-hidden">
        {/* Background decorative circles */}
        <div className="absolute top-0 right-0 w-72 h-72 bg-[#1E88E5] opacity-10 rounded-full -translate-y-1/2 translate-x-1/2" />
        <div className="absolute bottom-0 left-0 w-96 h-96 bg-[#1E88E5] opacity-5 rounded-full translate-y-1/2 -translate-x-1/2" />

        <div className="relative z-10">
          <Link to="/" className="flex items-center gap-3 mb-16">
            <div className="w-10 h-10 bg-[#1E88E5] rounded-xl flex items-center justify-center">
              <Car className="h-6 w-6 text-white" />
            </div>
            <span className="text-xl font-bold">Auto Consultancy</span>
          </Link>
          <h1 className="text-4xl font-extrabold leading-tight mb-6">
            Manage your bike finance journey — <span className="text-[#1E88E5]">effortlessly.</span>
          </h1>
          <p className="text-blue-200 text-lg leading-relaxed">
            Submit applications, upload documents, track status, and get expert consultancy — all in one place.
          </p>

          <div className="mt-10 space-y-4">
            {[
              { icon: '🏍️', text: '61 bike brands supported — Indian & international' },
              { icon: '📄', text: 'Secure document upload & verification' },
              { icon: '💰', text: 'Full finance & loan management' },
              { icon: '⚡', text: 'Real-time status tracking' },
            ].map((item, i) => (
              <div key={i} className="flex items-center gap-3 text-blue-100">
                <span className="text-xl">{item.icon}</span>
                <span className="text-sm">{item.text}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Testimonial */}
        <div className="relative z-10 p-6 bg-white/10 rounded-2xl backdrop-blur-sm border border-white/10">
          <p className="italic text-gray-200 text-sm leading-relaxed">
            "The fastest and most reliable auto consultancy service I've used. Got my NOC in 3 days!"
          </p>
          <div className="mt-4 flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-[#1E88E5] flex items-center justify-center font-bold text-sm">SK</div>
            <div>
              <p className="font-semibold text-white text-sm">Sanjay Kumar</p>
              <p className="text-xs text-gray-400">Verified Customer • Mumbai</p>
            </div>
          </div>
        </div>
      </div>

      {/* Right Login Form */}
      <div className="flex-1 bg-white p-8 lg:p-16 flex flex-col justify-center">
        <div className="max-w-md w-full mx-auto">
          {/* Mobile logo */}
          <div className="flex items-center gap-2 mb-8 lg:hidden">
            <div className="w-9 h-9 bg-[#1E88E5] rounded-lg flex items-center justify-center">
              <Car className="h-5 w-5 text-white" />
            </div>
            <span className="text-lg font-bold text-[#0F1B35]">Auto Consultancy</span>
          </div>

          <div className="mb-8">
            <h2 className="text-3xl font-bold text-gray-900">Sign In</h2>
            <p className="text-gray-600 mt-2 text-sm">
              Don't have an account?{' '}
              <Link to="/register" className="text-[#1E88E5] font-semibold hover:underline">
                Register as Customer
              </Link>
            </p>
          </div>

          {/* Demo credentials */}
          <div className="bg-blue-50 border border-blue-100 rounded-xl p-4 mb-6">
            <p className="text-xs font-bold text-blue-800 mb-2 flex items-center gap-1">
              <ShieldCheck size={14} /> Demo Credentials (click to fill)
            </p>
            <div className="flex flex-wrap gap-2">
              {['admin', 'worker', 'customer'].map((role) => (
                <button
                  key={role}
                  type="button"
                  onClick={() => fillDemo(role)}
                  className="text-xs px-3 py-1 bg-white border border-blue-200 rounded-full hover:bg-blue-100 capitalize font-medium text-blue-700 transition-colors"
                >
                  {role}
                </button>
              ))}
            </div>
          </div>

          {error && (
            <div className="bg-red-50 border-l-4 border-red-500 p-3 mb-5 rounded-r-lg">
              <p className="text-red-700 text-sm">{error}</p>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-5">
            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-1.5">Email Address</label>
              <input
                type="email" name="email" value={formData.email} onChange={handleChange} autoComplete="email"
                className="w-full px-4 py-3 rounded-xl border border-gray-200 focus:ring-2 focus:ring-[#1E88E5]/30 focus:border-[#1E88E5] outline-none transition-all bg-gray-50 focus:bg-white text-sm"
                placeholder="you@example.com"
              />
            </div>

            <div>
              <div className="flex justify-between items-center mb-1.5">
                <label className="block text-sm font-semibold text-gray-700">Password</label>
                <a href="#" className="text-xs text-[#1E88E5] hover:underline">Forgot password?</a>
              </div>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'} name="password" value={formData.password} onChange={handleChange}
                  className="w-full px-4 py-3 rounded-xl border border-gray-200 focus:ring-2 focus:ring-[#1E88E5]/30 focus:border-[#1E88E5] outline-none transition-all bg-gray-50 focus:bg-white text-sm pr-12"
                  placeholder="••••••••"
                />
                <button
                  type="button" onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 transition-colors"
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </div>

            <button
              type="submit" disabled={loading}
              className="w-full bg-[#1E88E5] hover:bg-[#1976D2] text-white py-3.5 rounded-xl font-bold transition-all duration-200 disabled:opacity-60 disabled:cursor-not-allowed shadow-md hover:shadow-lg hover:scale-[1.01] mt-2 text-sm"
            >
              {loading ? (
                <span className="flex items-center justify-center gap-2">
                  <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  Signing in...
                </span>
              ) : 'Sign In'}
            </button>
          </form>

          <p className="mt-8 text-center text-xs text-gray-400">
            By signing in, you agree to our{' '}
            <a href="#" className="text-[#1E88E5] hover:underline">Terms</a> and{' '}
            <a href="#" className="text-[#1E88E5] hover:underline">Privacy Policy</a>
          </p>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
