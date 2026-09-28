import React, { createContext, useState, useEffect, useContext } from 'react';
import * as authApi from '../api/authApi';

export const AuthContext = createContext();

// ── Storage helpers ──────────────────────────────────────────────────────────
// We keep the JWT in sessionStorage (cleared when the tab closes) rather than
// localStorage to reduce the window of exposure should an XSS attack occur.
// The user profile (non-secret metadata) is kept in localStorage so the UI
// can restore the user's name / role without requiring a fresh network call.
const STORAGE_TOKEN_KEY = 'ac_token';
const STORAGE_USER_KEY  = 'ac_user';

const readToken = () => sessionStorage.getItem(STORAGE_TOKEN_KEY);
const readUser  = () => {
  try { return JSON.parse(localStorage.getItem(STORAGE_USER_KEY)); } catch { return null; }
};

const writeSession = (token, userData) => {
  sessionStorage.setItem(STORAGE_TOKEN_KEY, token);
  localStorage.setItem(STORAGE_USER_KEY, JSON.stringify(userData));
};

const clearSession = () => {
  sessionStorage.removeItem(STORAGE_TOKEN_KEY);
  localStorage.removeItem(STORAGE_USER_KEY);
  // Also clear any legacy keys that may have been set by older versions
  localStorage.removeItem('token');
  localStorage.removeItem('user');
};

// ─────────────────────────────────────────────────────────────────────────────

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Restore session on page reload / tab re-open within same session
    const storedToken = readToken();
    const storedUser  = readUser();
    if (storedToken && storedUser) {
      setUser({ ...storedUser, token: storedToken });
    }
    setLoading(false);
  }, []);

  /**
   * Backend AuthResponse shape (wrapped in ApiResponse):
   *   { success: true, data: { token, email, role, firstName, lastName } }
   */
  const _handleAuthResponse = (responseData) => {
    const { token, email, role, firstName, lastName } = responseData.data.data;
    const userData = { email, role, firstName, lastName };  // no token in localStorage
    writeSession(token, userData);
    setUser({ ...userData, token });
    return { ...userData, token };
  };

  const login = async (email, password) => {
    const response = await authApi.login(email, password);
    return _handleAuthResponse(response);
  };

  const register = async (data) => {
    const response = await authApi.register(data);
    return _handleAuthResponse(response);
  };

  const logout = async () => {
    // Revoke the token server-side so it cannot be reused even if captured
    try {
      await authApi.logout().catch(() => {/* ignore network error on logout */});
    } finally {
      clearSession();
      setUser(null);
    }
  };

  // Re-sync auth state after profile update (e.g. name change)
  const refreshUser = (updatedFields = null) => {
    if (updatedFields) {
      const current = user || {};
      const merged  = { ...current, ...updatedFields };
      const { token, ...nonSecret } = merged;
      localStorage.setItem(STORAGE_USER_KEY, JSON.stringify(nonSecret));
      setUser(merged);
    } else {
      const stored = readUser();
      const token  = readToken();
      if (stored && token) setUser({ ...stored, token });
    }
  };

  const value = {
    user,
    isAuthenticated: !!user,
    isAdmin:    user?.role === 'ADMIN',
    isWorker:   user?.role === 'WORKER',
    isCustomer: user?.role === 'CUSTOMER',
    login,
    register,
    logout,
    refreshUser,
    loading,
  };

  return (
    <AuthContext.Provider value={value}>
      {!loading && children}
    </AuthContext.Provider>
  );
};
