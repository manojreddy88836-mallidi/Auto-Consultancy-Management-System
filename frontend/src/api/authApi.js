import api from './axios';

// ── Worker / Customer standard login ─────────────────────────────────────────
export const login    = (email, password) => api.post('/auth/login', { email, password });
export const register = (data)            => api.post('/auth/register', data);
export const logout   = ()                => api.post('/auth/logout');

// ── Admin two-step login ──────────────────────────────────────────────────────
/** Step 1: Validate admin credentials → returns { otpToken, sentTo, expiresIn } */
export const adminInitiateLogin = (email, password) =>
  api.post('/auth/admin/login', { email, password });

/** Step 2: Verify OTP → returns full AuthResponse with JWT */
export const adminVerifyOtp = (otpToken, otp) =>
  api.post('/auth/admin/verify-otp', { otpToken, otp });

/** Resend OTP (60-second cooldown enforced server-side) */
export const adminResendOtp = (otpToken, adminEmail) =>
  api.post('/auth/admin/resend-otp', { otpToken, adminEmail });
