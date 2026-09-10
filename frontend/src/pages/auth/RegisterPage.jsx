import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Bike, Eye, EyeOff, CheckCircle, ShieldCheck, User, Mail, Phone, Lock } from 'lucide-react';
import { toast } from 'react-hot-toast';
import useAuth from '../../hooks/useAuth';

const InputField = ({ label, name, type = 'text', value, onChange, error, placeholder, icon: Icon, children }) => (
  <div>
    <label className="block text-sm font-semibold text-gray-700 mb-1.5">{label}</label>
    <div className="relative">
      {Icon && <Icon size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none"/>}
      <input
        type={type} name={name} value={value} onChange={onChange}
        placeholder={placeholder} autoComplete={name}
        className={`w-full py-3 rounded-xl border text-sm outline-none transition-all bg-gray-50 focus:bg-white
          ${Icon ? 'pl-10' : 'pl-4'} pr-4
          ${error ? 'border-red-400 focus:ring-2 focus:ring-red-200' : 'border-gray-200 focus:ring-2 focus:ring-[#1E88E5]/30 focus:border-[#1E88E5]'}`}
      />
      {children}
    </div>
    {error && <p className="mt-1 text-xs text-red-500 font-medium">{error}</p>}
  </div>
);

const RegisterPage = () => {
  const navigate = useNavigate();
  const { register } = useAuth();
  const [showPwd, setShowPwd]       = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [loading, setLoading]       = useState(false);
  const [errors, setErrors]         = useState({});
  const [form, setForm] = useState({
    firstName: '', lastName: '', email: '', phone: '',
    password: '', confirmPassword: '',
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm(prev => ({ ...prev, [name]: value }));
    if (errors[name]) setErrors(prev => ({ ...prev, [name]: null }));
  };

  const validate = () => {
    const errs = {};
    if (!form.firstName.trim()) errs.firstName = 'First name is required';
    if (!form.lastName.trim())  errs.lastName  = 'Last name is required';
    if (!form.email.trim())     errs.email     = 'Email is required';
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) errs.email = 'Invalid email format';
    if (!form.phone.trim())     errs.phone     = 'Phone is required';
    else if (!/^[0-9]{10}$/.test(form.phone)) errs.phone = 'Phone must be 10 digits';
    if (!form.password)         errs.password  = 'Password is required';
    else if (form.password.length < 8) errs.password = 'Minimum 8 characters';
    else if (!/(?=.*[A-Z])(?=.*[0-9])/.test(form.password)) errs.password = 'Must contain uppercase and number';
    if (!form.confirmPassword)  errs.confirmPassword = 'Please confirm your password';
    else if (form.confirmPassword !== form.password) errs.confirmPassword = 'Passwords do not match';
    return errs;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const errs = validate();
    if (Object.keys(errs).length > 0) { setErrors(errs); return; }
    setLoading(true);
    try {
      const userData = await register({
        firstName: form.firstName.trim(),
        lastName:  form.lastName.trim(),
        email:     form.email.trim().toLowerCase(),
        phone:     form.phone.trim(),
        password:  form.password,
      });
      toast.success(`Welcome, ${userData.firstName}! Account created.`);
      navigate('/customer');
    } catch (err) {
      const msg = err?.response?.data?.message || 'Registration failed. Try again.';
      toast.error(msg);
      if (msg.toLowerCase().includes('email')) setErrors({ email: 'Email already in use' });
    } finally {
      setLoading(false);
    }
  };

  const strengthScore = () => {
    let s = 0;
    if (form.password.length >= 8) s++;
    if (/[A-Z]/.test(form.password)) s++;
    if (/[0-9]/.test(form.password)) s++;
    if (/[^A-Za-z0-9]/.test(form.password)) s++;
    return s;
  };
  const strength = strengthScore();
  const strengthLabel = ['','Weak','Fair','Good','Strong'][strength];
  const strengthColor = ['','bg-red-400','bg-yellow-400','bg-blue-400','bg-emerald-500'][strength];

  return (
    <div className="min-h-screen flex font-sans">
      {/* Left Branding Panel */}
      <div className="hidden lg:flex w-[42%] bg-[#0F1B35] text-white flex-col justify-between p-12 relative overflow-hidden">
        <div className="absolute top-0 right-0 w-80 h-80 bg-[#1E88E5] opacity-10 rounded-full -translate-y-1/3 translate-x-1/3"/>
        <div className="absolute bottom-0 left-0 w-96 h-96 bg-[#1E88E5] opacity-5 rounded-full translate-y-1/2 -translate-x-1/2"/>

        <div className="relative z-10">
          <Link to="/" className="flex items-center gap-3 mb-12">
            <div className="w-10 h-10 bg-[#1E88E5] rounded-xl flex items-center justify-center flex-shrink-0">
              <Bike size={22} className="text-white"/>
            </div>
            <span className="text-xl font-bold">Auto Consultancy</span>
          </Link>
          <h1 className="text-4xl font-extrabold leading-tight mb-5">
            Start your bike consultancy journey{' '}
            <span className="text-[#1E88E5]">today.</span>
          </h1>
          <p className="text-blue-200 text-base leading-relaxed mb-10">
            Create a free account and access all services — RC transfer, loan NOC, finance verification, and more.
          </p>
          <div className="space-y-4">
            {[
              { icon: '🏍️', text: 'Apply for RC transfer & ownership change' },
              { icon: '📄', text: 'Upload documents securely' },
              { icon: '💰', text: 'Get loan closure & NOC assistance' },
              { icon: '⚡', text: 'Real-time status tracking' },
              { icon: '👨‍💼', text: 'Dedicated expert assigned to your case' },
            ].map((item, i) => (
              <div key={i} className="flex items-center gap-3 text-blue-100">
                <span className="text-xl">{item.icon}</span>
                <span className="text-sm">{item.text}</span>
              </div>
            ))}
          </div>
        </div>

        <div className="relative z-10 p-5 bg-white/10 rounded-2xl backdrop-blur-sm border border-white/10">
          <div className="flex items-start gap-3">
            <div className="w-10 h-10 rounded-full bg-[#1E88E5] flex items-center justify-center font-bold text-sm flex-shrink-0">RK</div>
            <div>
              <p className="italic text-gray-200 text-sm leading-relaxed">
                "Got my bike's loan NOC and RC transfer done in just 5 days. Excellent service!"
              </p>
              <p className="font-semibold text-white text-sm mt-2">Rahul Krishnan</p>
              <p className="text-xs text-gray-400">Verified Customer • Pune</p>
            </div>
          </div>
        </div>
      </div>

      {/* Right Registration Form */}
      <div className="flex-1 bg-white overflow-y-auto flex flex-col">
        <div className="flex-1 flex flex-col justify-center p-8 lg:p-14">
          <div className="max-w-md w-full mx-auto">
            {/* Mobile logo */}
            <div className="flex items-center gap-2 mb-8 lg:hidden">
              <div className="w-9 h-9 bg-[#1E88E5] rounded-lg flex items-center justify-center">
                <Bike size={18} className="text-white"/>
              </div>
              <span className="text-lg font-bold text-[#0F1B35]">Auto Consultancy</span>
            </div>

            <div className="mb-7">
              <h2 className="text-3xl font-bold text-gray-900">Create Account</h2>
              <p className="text-gray-500 mt-1.5 text-sm">
                Already have an account?{' '}
                <Link to="/login" className="text-[#1E88E5] font-semibold hover:underline">Sign in</Link>
              </p>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">
              {/* Name row */}
              <div className="grid grid-cols-2 gap-3">
                <InputField label="First Name" name="firstName" value={form.firstName}
                  onChange={handleChange} error={errors.firstName} placeholder="Rahul" icon={User}/>
                <InputField label="Last Name" name="lastName" value={form.lastName}
                  onChange={handleChange} error={errors.lastName} placeholder="Kumar"/>
              </div>

              <InputField label="Email Address" name="email" type="email" value={form.email}
                onChange={handleChange} error={errors.email} placeholder="you@example.com" icon={Mail}/>

              <InputField label="Mobile Number" name="phone" type="tel" value={form.phone}
                onChange={handleChange} error={errors.phone} placeholder="10-digit number" icon={Phone}/>

              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-1.5">Password</label>
                <div className="relative">
                  <Lock size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none"/>
                  <input type={showPwd ? 'text' : 'password'} name="password" value={form.password}
                    onChange={handleChange} placeholder="Min 8 chars with uppercase & number"
                    className={`w-full pl-10 pr-11 py-3 rounded-xl border text-sm outline-none transition-all bg-gray-50 focus:bg-white
                      ${errors.password ? 'border-red-400 focus:ring-2 focus:ring-red-200' : 'border-gray-200 focus:ring-2 focus:ring-[#1E88E5]/30 focus:border-[#1E88E5]'}`}/>
                  <button type="button" onClick={() => setShowPwd(v => !v)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600">
                    {showPwd ? <EyeOff size={17}/> : <Eye size={17}/>}
                  </button>
                </div>
                {form.password && (
                  <div className="mt-1.5 flex items-center gap-2">
                    <div className="flex-1 h-1.5 bg-gray-100 rounded-full overflow-hidden">
                      <div className={`h-full rounded-full transition-all ${strengthColor}`} style={{ width: `${(strength/4)*100}%` }}/>
                    </div>
                    <span className={`text-xs font-semibold ${strengthColor.replace('bg-','text-')}`}>{strengthLabel}</span>
                  </div>
                )}
                {errors.password && <p className="mt-1 text-xs text-red-500 font-medium">{errors.password}</p>}
              </div>

              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-1.5">Confirm Password</label>
                <div className="relative">
                  <Lock size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none"/>
                  <input type={showConfirm ? 'text' : 'password'} name="confirmPassword" value={form.confirmPassword}
                    onChange={handleChange} placeholder="Re-enter password"
                    className={`w-full pl-10 pr-11 py-3 rounded-xl border text-sm outline-none transition-all bg-gray-50 focus:bg-white
                      ${errors.confirmPassword ? 'border-red-400 focus:ring-2 focus:ring-red-200' : 'border-gray-200 focus:ring-2 focus:ring-[#1E88E5]/30 focus:border-[#1E88E5]'}`}/>
                  <button type="button" onClick={() => setShowConfirm(v => !v)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600">
                    {showConfirm ? <EyeOff size={17}/> : <Eye size={17}/>}
                  </button>
                </div>
                {form.confirmPassword && form.confirmPassword === form.password && (
                  <p className="mt-1 text-xs text-emerald-500 font-medium flex items-center gap-1">
                    <CheckCircle size={11}/> Passwords match
                  </p>
                )}
                {errors.confirmPassword && <p className="mt-1 text-xs text-red-500 font-medium">{errors.confirmPassword}</p>}
              </div>

              <button type="submit" disabled={loading}
                className="w-full bg-[#1E88E5] hover:bg-[#1976D2] text-white py-3.5 rounded-xl font-bold transition-all disabled:opacity-60 disabled:cursor-not-allowed shadow-md hover:shadow-lg hover:scale-[1.01] text-sm mt-2">
                {loading ? (
                  <span className="flex items-center justify-center gap-2">
                    <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"/>
                    Creating account…
                  </span>
                ) : 'Create Account'}
              </button>

              <div className="flex items-start gap-2 pt-1">
                <ShieldCheck size={15} className="text-emerald-500 flex-shrink-0 mt-0.5"/>
                <p className="text-xs text-gray-400">
                  Your data is encrypted and secure. By registering you agree to our{' '}
                  <a href="#" className="text-[#1E88E5] hover:underline">Terms</a> and{' '}
                  <a href="#" className="text-[#1E88E5] hover:underline">Privacy Policy</a>.
                </p>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
};

export default RegisterPage;
