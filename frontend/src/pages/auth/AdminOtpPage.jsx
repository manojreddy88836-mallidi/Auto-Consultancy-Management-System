import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { Car, ShieldCheck, Mail, RefreshCw, ArrowLeft, AlertCircle } from 'lucide-react';
import { toast } from 'react-hot-toast';
import useAuth from '../../hooks/useAuth';
import * as authApi from '../../api/authApi';

const RESEND_COOLDOWN = 60; // seconds

const AdminOtpPage = () => {
  const navigate  = useNavigate();
  const location  = useLocation();
  const { adminCompleteLogin } = useAuth();

  // ── State from navigation ──────────────────────────────────────────────────
  const { otpToken: initialToken, sentTo, expiresIn: initialExpiresIn, adminEmail } =
    location.state || {};

  const [otpToken,  setOtpToken]  = useState(initialToken || '');
  const [otp,       setOtp]       = useState(['', '', '', '', '', '']);
  const [loading,   setLoading]   = useState(false);
  const [error,     setError]     = useState('');
  const [resending, setResending] = useState(false);

  // ── Countdown timers ───────────────────────────────────────────────────────
  const [expiryLeft,  setExpiryLeft]  = useState(initialExpiresIn || 300); // 5 min
  const [resendLeft,  setResendLeft]  = useState(RESEND_COOLDOWN);
  const [canResend,   setCanResend]   = useState(false);

  const inputRefs = useRef([]);

  // Redirect away if accessed directly without state
  useEffect(() => {
    if (!initialToken || !adminEmail) {
      navigate('/login', { replace: true });
    }
  }, [initialToken, adminEmail, navigate]);

  // OTP expiry countdown
  useEffect(() => {
    if (expiryLeft <= 0) return;
    const id = setInterval(() => setExpiryLeft(p => Math.max(0, p - 1)), 1000);
    return () => clearInterval(id);
  }, [expiryLeft]);

  // Resend cooldown countdown
  useEffect(() => {
    if (canResend) return;
    if (resendLeft <= 0) { setCanResend(true); return; }
    const id = setInterval(() => setResendLeft(p => {
      if (p <= 1) { setCanResend(true); return 0; }
      return p - 1;
    }), 1000);
    return () => clearInterval(id);
  }, [resendLeft, canResend]);

  const formatTime = (secs) => {
    const m = Math.floor(secs / 60);
    const s = secs % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  };

  // ── OTP input handling ─────────────────────────────────────────────────────
  const handleOtpChange = (index, value) => {
    const digit = value.replace(/\D/g, '').slice(-1);
    const newOtp = [...otp];
    newOtp[index] = digit;
    setOtp(newOtp);
    setError('');

    // Auto-advance to next field
    if (digit && index < 5) {
      inputRefs.current[index + 1]?.focus();
    }
    // Auto-submit when all 6 digits entered
    if (digit && index === 5 && newOtp.every(d => d !== '')) {
      handleVerify(newOtp.join(''));
    }
  };

  const handleOtpKeyDown = (index, e) => {
    if (e.key === 'Backspace' && !otp[index] && index > 0) {
      inputRefs.current[index - 1]?.focus();
    }
  };

  const handleOtpPaste = (e) => {
    e.preventDefault();
    const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, 6);
    if (!pasted) return;
    const newOtp = [...Array(6)].map((_, i) => pasted[i] || '');
    setOtp(newOtp);
    if (pasted.length === 6) handleVerify(pasted);
    else inputRefs.current[pasted.length]?.focus();
  };

  // ── Verify OTP ────────────────────────────────────────────────────────────
  const handleVerify = useCallback(async (otpString) => {
    const code = otpString || otp.join('');
    if (code.length !== 6) {
      setError('Please enter all 6 digits of your verification code.');
      return;
    }
    setLoading(true);
    setError('');
    try {
      const userData = await adminCompleteLogin(otpToken, code);
      toast.success(`Welcome back, ${userData.firstName}! 🎉`);
      navigate('/admin', { replace: true });
    } catch (err) {
      const msg = err?.response?.data?.message || 'Invalid verification code. Please try again.';
      setError(msg);
      // Clear OTP inputs on failure
      setOtp(['', '', '', '', '', '']);
      setTimeout(() => inputRefs.current[0]?.focus(), 100);

      // If locked or expired, go back to login
      if (msg.includes('restart') || msg.includes('expired')) {
        setTimeout(() => navigate('/login', { replace: true }), 2500);
      }
    } finally {
      setLoading(false);
    }
  }, [otp, otpToken, adminCompleteLogin, navigate]);

  const handleSubmit = (e) => {
    e.preventDefault();
    handleVerify();
  };

  // ── Resend OTP ────────────────────────────────────────────────────────────
  const handleResend = async () => {
    if (!canResend) return;
    setResending(true);
    setError('');
    try {
      const response = await authApi.adminResendOtp(otpToken, adminEmail);
      const challenge = response.data.data;
      setOtpToken(challenge.otpToken);
      setExpiryLeft(challenge.expiresIn || 300);
      setResendLeft(RESEND_COOLDOWN);
      setCanResend(false);
      setOtp(['', '', '', '', '', '']);
      toast.success('New verification code sent!');
      setTimeout(() => inputRefs.current[0]?.focus(), 100);
    } catch (err) {
      const msg = err?.response?.data?.message || 'Failed to resend OTP. Please try again.';
      setError(msg);
      toast.error(msg);
    } finally {
      setResending(false);
    }
  };

  // ── Render ────────────────────────────────────────────────────────────────
  return (
    <div className="min-h-screen flex font-sans">
      {/* Left Branding Panel */}
      <div className="hidden lg:flex w-[42%] bg-[#0F1B35] text-white flex-col justify-between p-12 relative overflow-hidden">
        <div className="absolute top-0 right-0 w-72 h-72 bg-[#1E88E5] opacity-10 rounded-full -translate-y-1/2 translate-x-1/2" />
        <div className="absolute bottom-0 left-0 w-96 h-96 bg-[#1E88E5] opacity-5 rounded-full translate-y-1/2 -translate-x-1/2" />

        <div className="relative z-10">
          <Link to="/" className="flex items-center gap-3 mb-16">
            <div className="w-10 h-10 bg-[#1E88E5] rounded-xl flex items-center justify-center">
              <Car className="h-6 w-6 text-white" />
            </div>
            <span className="text-xl font-bold">Auto Consultancy</span>
          </Link>

          <div className="flex items-center gap-4 mb-8">
            <div className="w-16 h-16 bg-[#1E88E5] rounded-2xl flex items-center justify-center">
              <ShieldCheck className="h-9 w-9 text-white" />
            </div>
            <div>
              <h1 className="text-2xl font-extrabold">Two-Factor</h1>
              <h1 className="text-2xl font-extrabold text-[#1E88E5]">Authentication</h1>
            </div>
          </div>

          <p className="text-blue-200 text-base leading-relaxed mb-8">
            Your admin account is protected by Gmail OTP verification. This extra step ensures only you can access the admin dashboard.
          </p>

          <div className="space-y-4">
            {[
              { icon: '🔐', text: 'Cryptographically secure 6-digit OTP' },
              { icon: '⏱️', text: 'Expires in 5 minutes for your safety' },
              { icon: '🚫', text: 'Single use — invalidated after verification' },
              { icon: '📧', text: 'Sent directly to your registered Gmail' },
            ].map((item, i) => (
              <div key={i} className="flex items-center gap-3 text-blue-100">
                <span className="text-lg">{item.icon}</span>
                <span className="text-sm">{item.text}</span>
              </div>
            ))}
          </div>
        </div>

        <div className="relative z-10 p-5 bg-white/10 rounded-2xl backdrop-blur-sm border border-white/10">
          <p className="text-yellow-300 text-xs font-semibold mb-1">⚠️ Security Reminder</p>
          <p className="text-gray-200 text-sm">
            Never share this code with anyone. Auto Consultancy staff will never ask for your OTP.
          </p>
        </div>
      </div>

      {/* Right OTP Form */}
      <div className="flex-1 bg-white p-8 lg:p-16 flex flex-col justify-center">
        <div className="max-w-md w-full mx-auto">
          {/* Mobile logo */}
          <div className="flex items-center gap-2 mb-8 lg:hidden">
            <div className="w-9 h-9 bg-[#1E88E5] rounded-lg flex items-center justify-center">
              <Car className="h-5 w-5 text-white" />
            </div>
            <span className="text-lg font-bold text-[#0F1B35]">Auto Consultancy</span>
          </div>

          {/* Back link */}
          <Link
            to="/login"
            className="inline-flex items-center gap-2 text-sm text-gray-500 hover:text-[#1E88E5] transition-colors mb-8"
          >
            <ArrowLeft size={16} /> Back to Login
          </Link>

          {/* Header */}
          <div className="mb-8">
            <div className="w-14 h-14 bg-blue-50 rounded-2xl flex items-center justify-center mb-4">
              <Mail className="h-7 w-7 text-[#1E88E5]" />
            </div>
            <h2 className="text-3xl font-bold text-gray-900">Admin Verification</h2>
            <p className="text-gray-500 mt-2 text-sm leading-relaxed">
              Enter the 6-digit verification code sent to{' '}
              <span className="font-semibold text-gray-700">{sentTo || 'your Gmail'}</span>
            </p>
          </div>

          {/* OTP expiry timer */}
          <div className={`flex items-center gap-2 text-sm mb-6 ${
            expiryLeft < 60 ? 'text-red-500' : 'text-gray-500'
          }`}>
            <span>⏱</span>
            <span>
              {expiryLeft > 0
                ? <>Code expires in <strong>{formatTime(expiryLeft)}</strong></>
                : <strong className="text-red-600">Code expired — please resend</strong>
              }
            </span>
          </div>

          {/* Error display */}
          {error && (
            <div className="bg-red-50 border border-red-200 rounded-xl p-4 mb-6 flex items-start gap-3">
              <AlertCircle className="h-5 w-5 text-red-500 flex-shrink-0 mt-0.5" />
              <p className="text-red-700 text-sm">{error}</p>
            </div>
          )}

          {/* OTP inputs */}
          <form onSubmit={handleSubmit}>
            <div className="flex gap-3 justify-center mb-8" onPaste={handleOtpPaste}>
              {otp.map((digit, index) => (
                <input
                  key={index}
                  ref={el => inputRefs.current[index] = el}
                  type="text"
                  inputMode="numeric"
                  maxLength={1}
                  value={digit}
                  onChange={e => handleOtpChange(index, e.target.value)}
                  onKeyDown={e => handleOtpKeyDown(index, e)}
                  disabled={loading || expiryLeft <= 0}
                  className={`w-12 h-14 text-center text-2xl font-bold rounded-xl border-2 outline-none transition-all
                    ${digit ? 'border-[#1E88E5] bg-blue-50 text-[#0F1B35]' : 'border-gray-200 bg-gray-50 text-gray-800'}
                    focus:border-[#1E88E5] focus:bg-blue-50 focus:ring-2 focus:ring-[#1E88E5]/20
                    disabled:opacity-50 disabled:cursor-not-allowed`}
                  autoFocus={index === 0}
                />
              ))}
            </div>

            {/* Verify Button */}
            <button
              type="submit"
              disabled={loading || otp.some(d => !d) || expiryLeft <= 0}
              className="w-full bg-[#1E88E5] hover:bg-[#1976D2] text-white py-3.5 rounded-xl font-bold transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed shadow-md hover:shadow-lg hover:scale-[1.01] text-sm mb-4"
            >
              {loading ? (
                <span className="flex items-center justify-center gap-2">
                  <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  Verifying...
                </span>
              ) : (
                <span className="flex items-center justify-center gap-2">
                  <ShieldCheck size={16} />
                  Verify & Sign In
                </span>
              )}
            </button>
          </form>

          {/* Resend */}
          <div className="text-center">
            <p className="text-sm text-gray-500 mb-2">Didn't receive the code?</p>
            <button
              type="button"
              onClick={handleResend}
              disabled={!canResend || resending}
              className={`inline-flex items-center gap-2 text-sm font-semibold transition-all
                ${canResend
                  ? 'text-[#1E88E5] hover:text-[#1565C0] cursor-pointer'
                  : 'text-gray-400 cursor-not-allowed'}`}
            >
              <RefreshCw size={14} className={resending ? 'animate-spin' : ''} />
              {resending
                ? 'Sending...'
                : canResend
                  ? 'Resend verification code'
                  : `Resend in ${resendLeft}s`
              }
            </button>
          </div>

          <p className="mt-10 text-center text-xs text-gray-400">
            🔒 Admin access is protected by two-factor authentication
          </p>
        </div>
      </div>
    </div>
  );
};

export default AdminOtpPage;
