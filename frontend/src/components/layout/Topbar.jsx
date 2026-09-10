import React, { useState, useEffect, useRef, useCallback } from 'react';
import { Bell, User, X, CheckCheck, AlertTriangle } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import {
  getNotifications,
  markAsRead,
  markAllAsRead,
  getUnreadCount
} from '../../api/notificationApi';

// Format relative time
const relTime = (dt) => {
  if (!dt) return '';
  const diff = Date.now() - new Date(dt).getTime();
  const m = Math.floor(diff / 60000);
  if (m < 1)  return 'just now';
  if (m < 60) return `${m}m ago`;
  const h = Math.floor(m / 60);
  if (h < 24) return `${h}h ago`;
  return `${Math.floor(h / 24)}d ago`;
};

// EMI notification icon & color
const notifStyle = (type) => {
  if (type === 'EMI_CRITICAL') return { icon: '🔴', bg: 'bg-red-50 border-red-100',   title: 'text-red-700' };
  if (type === 'EMI_WARNING')  return { icon: '🟠', bg: 'bg-orange-50 border-orange-100', title: 'text-orange-700' };
  return { icon: '🔔', bg: 'bg-blue-50 border-blue-100', title: 'text-blue-700' };
};

export const Topbar = () => {
  const { user } = useAuth();
  const [open,         setOpen]         = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [unreadCount,  setUnreadCount]  = useState(0);
  const [loadingNotif, setLoadingNotif] = useState(false);
  const dropRef = useRef(null);

  // ── Fetch unread count (lightweight poll every 60s) ──────────────────────
  const fetchCount = useCallback(async () => {
    try {
      const res = await getUnreadCount();
      setUnreadCount(Number(res.data?.data ?? 0));
    } catch { /* silent */ }
  }, []);

  useEffect(() => {
    fetchCount();
    const interval = setInterval(fetchCount, 60000);
    return () => clearInterval(interval);
  }, [fetchCount]);

  // ── Fetch full notification list when dropdown opens ─────────────────────
  const fetchNotifications = useCallback(async () => {
    setLoadingNotif(true);
    try {
      const res = await getNotifications();
      const list = (res.data?.data || []).slice(0, 30); // cap at 30
      setNotifications(list);
      setUnreadCount(list.filter(n => !n.read).length);
    } catch { /* silent */ }
    finally { setLoadingNotif(false); }
  }, []);

  const handleOpen = () => {
    setOpen(o => !o);
    if (!open) fetchNotifications();
  };

  // ── Close on outside click ───────────────────────────────────────────────
  useEffect(() => {
    const handler = (e) => {
      if (dropRef.current && !dropRef.current.contains(e.target)) setOpen(false);
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  // ── Mark individual as read ──────────────────────────────────────────────
  const handleMarkRead = async (id) => {
    try {
      await markAsRead(id);
      setNotifications(prev => prev.map(n => n.id === id ? { ...n, read: true } : n));
      setUnreadCount(c => Math.max(0, c - 1));
    } catch { /* silent */ }
  };

  // ── Mark all as read ─────────────────────────────────────────────────────
  const handleMarkAll = async () => {
    try {
      await markAllAsRead();
      setNotifications(prev => prev.map(n => ({ ...n, read: true })));
      setUnreadCount(0);
    } catch { /* silent */ }
  };

  // Separate EMI alerts from other notifications for grouped display
  const emiCritical = notifications.filter(n => n.type === 'EMI_CRITICAL');
  const emiWarning  = notifications.filter(n => n.type === 'EMI_WARNING');
  const other       = notifications.filter(n => n.type !== 'EMI_CRITICAL' && n.type !== 'EMI_WARNING');

  return (
    <header className="h-16 bg-white border-b border-slate-200 flex items-center justify-between px-6 relative z-40">
      <div/>

      <div className="flex items-center gap-4">

        {/* ── Notification Bell ────────────────────────────────────────── */}
        <div className="relative" ref={dropRef}>
          <button
            onClick={handleOpen}
            className="relative text-slate-500 hover:text-slate-700 p-1.5 hover:bg-gray-100 rounded-lg transition-colors"
            aria-label="Notifications"
          >
            <Bell size={20} />
            {unreadCount > 0 && (
              <span className="absolute -top-1 -right-1 min-w-[18px] h-[18px] bg-red-500 text-white text-[10px] font-bold rounded-full flex items-center justify-center px-1">
                {unreadCount > 99 ? '99+' : unreadCount}
              </span>
            )}
          </button>

          {/* Dropdown panel */}
          {open && (
            <div className="absolute right-0 top-10 w-96 bg-white rounded-2xl shadow-2xl border border-gray-100 overflow-hidden">

              {/* Dropdown header */}
              <div className="flex items-center justify-between px-4 py-3 border-b border-gray-100 bg-gray-50">
                <div className="flex items-center gap-2">
                  <Bell size={15} className="text-gray-600" />
                  <span className="font-bold text-sm text-gray-800">Notifications</span>
                  {unreadCount > 0 && (
                    <span className="bg-red-500 text-white text-[10px] font-bold px-1.5 py-0.5 rounded-full">
                      {unreadCount}
                    </span>
                  )}
                </div>
                <div className="flex items-center gap-2">
                  {unreadCount > 0 && (
                    <button
                      onClick={handleMarkAll}
                      className="text-xs text-blue-600 hover:underline flex items-center gap-0.5"
                    >
                      <CheckCheck size={12} /> Mark all read
                    </button>
                  )}
                  <button onClick={() => setOpen(false)} className="p-0.5 hover:bg-gray-200 rounded">
                    <X size={14} className="text-gray-500" />
                  </button>
                </div>
              </div>

              {/* Notification list */}
              <div className="max-h-[420px] overflow-y-auto">
                {loadingNotif ? (
                  <div className="text-center py-8 text-gray-400 text-sm">Loading...</div>
                ) : notifications.length === 0 ? (
                  <div className="text-center py-8 text-gray-400 text-sm">No notifications</div>
                ) : (
                  <>
                    {/* ── Critical EMI alerts ─────────────────────────── */}
                    {emiCritical.length > 0 && (
                      <div>
                        <div className="px-4 py-2 bg-red-50 border-b border-red-100">
                          <p className="text-xs font-bold text-red-600 uppercase tracking-wide">🔴 Critical — EMI Overdue</p>
                        </div>
                        {emiCritical.map(n => (
                          <NotifItem key={n.id} n={n} onRead={handleMarkRead} />
                        ))}
                      </div>
                    )}

                    {/* ── Warning EMI alerts ──────────────────────────── */}
                    {emiWarning.length > 0 && (
                      <div>
                        <div className="px-4 py-2 bg-orange-50 border-b border-orange-100">
                          <p className="text-xs font-bold text-orange-600 uppercase tracking-wide">🟠 Warning — EMI Overdue</p>
                        </div>
                        {emiWarning.map(n => (
                          <NotifItem key={n.id} n={n} onRead={handleMarkRead} />
                        ))}
                      </div>
                    )}

                    {/* ── Other notifications ─────────────────────────── */}
                    {other.length > 0 && (
                      <div>
                        {(emiCritical.length > 0 || emiWarning.length > 0) && (
                          <div className="px-4 py-2 bg-gray-50 border-b border-gray-100">
                            <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide">Other</p>
                          </div>
                        )}
                        {other.map(n => (
                          <NotifItem key={n.id} n={n} onRead={handleMarkRead} />
                        ))}
                      </div>
                    )}
                  </>
                )}
              </div>
            </div>
          )}
        </div>

        {/* ── User avatar ─────────────────────────────────────────────── */}
        <div className="flex items-center gap-2 text-slate-700">
          <div className="w-8 h-8 rounded-full bg-primary-100 flex items-center justify-center text-primary-700 font-semibold">
            <User size={16} />
          </div>
          <span className="font-medium">
            {user ? `${user.firstName || ''} ${user.lastName || ''}`.trim() || 'Admin' : 'Admin User'}
          </span>
        </div>
      </div>
    </header>
  );
};

// ── Single notification item ──────────────────────────────────────────────────
function NotifItem({ n, onRead }) {
  const s = notifStyle(n.type);
  return (
    <div
      className={`px-4 py-3 border-b border-gray-50 cursor-pointer hover:bg-gray-50 transition-colors ${!n.read ? 'bg-blue-50/30' : ''}`}
      onClick={() => !n.read && onRead(n.id)}
    >
      <div className="flex items-start gap-2">
        <span className="text-base flex-shrink-0 mt-0.5">{s.icon}</span>
        <div className="flex-1 min-w-0">
          <div className="flex items-start justify-between gap-2">
            <p className={`text-xs font-bold leading-tight ${s.title} truncate`}>{n.title}</p>
            {!n.read && <span className="w-2 h-2 bg-blue-500 rounded-full flex-shrink-0 mt-1"/>}
          </div>
          <p className="text-xs text-gray-600 mt-0.5 whitespace-pre-line leading-snug line-clamp-3">{n.message}</p>
          <p className="text-[10px] text-gray-400 mt-1">{relTime(n.createdAt)}</p>
        </div>
      </div>
    </div>
  );
}

export default Topbar;
