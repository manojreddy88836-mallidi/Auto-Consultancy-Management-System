import React, { createContext, useState, useEffect, useContext } from 'react';
import * as authApi from '../api/authApi';

export const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const storedUser = localStorage.getItem('user');
    const storedToken = localStorage.getItem('token');
    if (storedUser && storedToken) {
      try {
        setUser(JSON.parse(storedUser));
      } catch {
        localStorage.removeItem('user');
        localStorage.removeItem('token');
      }
    }
    setLoading(false);
  }, []);

  /**
   * Backend AuthResponse shape (flat, wrapped in ApiResponse):
   * { success: true, data: { token, email, role, firstName, lastName } }
   * We build a normalized user object to store in state + localStorage.
   */
  const _handleAuthResponse = (responseData) => {
    const { token, email, role, firstName, lastName } = responseData.data.data;
    const userData = { token, email, role, firstName, lastName };
    localStorage.setItem('token', token);
    localStorage.setItem('user', JSON.stringify(userData));
    setUser(userData);
    return userData;
  };

  const login = async (email, password) => {
    const response = await authApi.login(email, password);
    return _handleAuthResponse(response);
  };

  const register = async (data) => {
    const response = await authApi.register(data);
    return _handleAuthResponse(response);
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setUser(null);
  };

  // Re-sync auth state after profile update (e.g. name change)
  const refreshUser = (updatedFields = null) => {
    if (updatedFields) {
      // Merge updated fields into stored user
      const current = user || {};
      const merged = { ...current, ...updatedFields };
      localStorage.setItem('user', JSON.stringify(merged));
      setUser(merged);
    } else {
      try {
        const stored = localStorage.getItem('user');
        if (stored) setUser(JSON.parse(stored));
      } catch { /* ignore */ }
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
