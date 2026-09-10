import React, { useState, useEffect, useCallback } from 'react';
import { Users, Search, Eye, MapPin, Phone, Mail, RefreshCw } from 'lucide-react';
import { toast } from 'react-hot-toast';
import { getCustomers } from '../../api/adminApi';
import { useNavigate } from 'react-router-dom';

const CustomerDetailModal = ({ customer, onClose }) => {
  if (!customer) return null;
  const u = customer.user || customer;
  const c = customer;
  const Row = ({ label, value }) => (
    <div className="flex justify-between items-start py-2 border-b border-gray-50 last:border-0">
      <span className="text-xs text-gray-500 font-medium uppercase tracking-wide">{label}</span>
      <span className="text-sm font-semibold text-gray-800 text-right max-w-[55%]">{value || '—'}</span>
    </div>
  );
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={onClose}/>
      <div className="relative bg-white rounded-2xl shadow-2xl w-full max-w-md mx-4 p-6 z-10 max-h-[90vh] overflow-y-auto">
        <div className="flex items-center gap-4 mb-6">
          <div className="w-14 h-14 rounded-full bg-gradient-to-br from-[#1E88E5] to-[#0F1B35] flex items-center justify-center text-white text-lg font-bold flex-shrink-0">
            {u.firstName?.[0]}{u.lastName?.[0]}
          </div>
          <div>
            <h3 className="text-lg font-bold text-gray-900">{u.firstName} {u.lastName}</h3>
            <p className="text-sm text-gray-500">{u.email}</p>
            <span className={`inline-block mt-1 px-2 py-0.5 rounded-full text-xs font-semibold ${u.active !== false ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-600'}`}>
              {u.active !== false ? 'Active' : 'Inactive'}
            </span>
          </div>
        </div>

        <div className="space-y-0 mb-6">
          <Row label="Phone"        value={u.phone}/>
          <Row label="City"         value={c.city}/>
          <Row label="State"        value={c.state}/>
          <Row label="Pincode"      value={c.pincode}/>
          <Row label="Address"      value={c.address}/>
          <Row label="Date of Birth" value={c.dateOfBirth}/>
          <Row label="ID Proof"     value={c.identityProof}/>
          <Row label="ID Number"    value={c.identityProofNumber}/>
          <Row label="Profile"      value={c.profileComplete ? '✅ Complete' : '⚠️ Incomplete'}/>
          <Row label="Joined"       value={u.createdAt ? new Date(u.createdAt).toLocaleDateString('en-IN') : '—'}/>
        </div>

        <button onClick={onClose}
          className="w-full py-2.5 rounded-xl border border-gray-200 text-sm font-medium hover:bg-gray-50 transition-colors">
          Close
        </button>
      </div>
    </div>
  );
};

const CustomersPage = () => {
  const navigate = useNavigate();
  const [customers, setCustomers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [selected, setSelected] = useState(null);
  const PAGE_SIZE = 12;

  const fetchCustomers = useCallback(async () => {
    setLoading(true);
    try {
      const res = await getCustomers({ page, size: PAGE_SIZE, sort: 'user.createdAt,desc' });
      const d = res.data?.data;
      if (d?.content !== undefined) {
        setCustomers(d.content);
        setTotalPages(d.totalPages || 1);
        setTotalElements(d.totalElements || 0);
      } else {
        const arr = Array.isArray(d) ? d : [];
        setCustomers(arr);
        setTotalPages(1);
        setTotalElements(arr.length);
      }
    } catch { toast.error('Failed to load customers'); }
    finally { setLoading(false); }
  }, [page]);

  useEffect(() => { fetchCustomers(); }, [fetchCustomers]);

  const filtered = customers.filter(c => {
    const q = search.toLowerCase();
    const u = c.user || c;
    return !q || (u.firstName + ' ' + u.lastName).toLowerCase().includes(q)
      || u.email?.toLowerCase().includes(q) || u.phone?.includes(q)
      || c.city?.toLowerCase().includes(q);
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            <Users className="text-[#1E88E5]" size={24}/> Customers
          </h1>
          <p className="text-gray-500 text-sm mt-1">{totalElements} registered customers</p>
        </div>
        <button onClick={fetchCustomers} className="p-2 hover:bg-gray-100 rounded-lg self-start" title="Refresh">
          <RefreshCw size={18} className="text-gray-500"/>
        </button>
      </div>

      <div className="relative max-w-sm">
        <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"/>
        <input value={search} onChange={e => setSearch(e.target.value)} placeholder="Search by name, email, city..."
          className="w-full pl-9 pr-4 py-2.5 rounded-xl border border-gray-200 text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none"/>
      </div>

      {loading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
          {[1,2,3,4,5,6].map(i => <div key={i} className="bg-white rounded-2xl h-36 animate-pulse border border-gray-100"/>)}
        </div>
      ) : filtered.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-20 text-gray-400 bg-white rounded-2xl border border-gray-100">
          <Users size={40} className="mb-3 opacity-25"/>
          <p className="text-sm font-medium">{search ? 'No customers match your search' : 'No customers registered yet'}</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
          {filtered.map(c => {
            const u = c.user || c;
            return (
              <div key={c.id} className="bg-white rounded-2xl border border-gray-100 shadow-sm p-5 hover:shadow-md transition-shadow">
                <div className="flex items-center gap-3 mb-3">
                  <div className="w-10 h-10 rounded-full bg-gradient-to-br from-blue-100 to-blue-200 flex items-center justify-center text-[#1E88E5] font-bold text-sm flex-shrink-0">
                    {u.firstName?.[0]}{u.lastName?.[0]}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="font-bold text-gray-900 truncate">{u.firstName} {u.lastName}</p>
                    <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${c.profileComplete ? 'bg-green-100 text-green-600' : 'bg-amber-100 text-amber-600'}`}>
                      {c.profileComplete ? 'Profile Complete' : 'Profile Incomplete'}
                    </span>
                  </div>
                </div>
                <div className="space-y-1.5 text-sm text-gray-500">
                  <div className="flex items-center gap-2"><Mail size={12} className="text-gray-400 flex-shrink-0"/><span className="truncate text-xs">{u.email}</span></div>
                  {u.phone && <div className="flex items-center gap-2"><Phone size={12} className="text-gray-400"/><span className="text-xs">{u.phone}</span></div>}
                  {c.city && <div className="flex items-center gap-2"><MapPin size={12} className="text-gray-400"/><span className="text-xs">{c.city}{c.state ? `, ${c.state}` : ''}</span></div>}
                </div>
                <div className="flex items-center gap-2 mt-4 pt-3 border-t border-gray-100">
                  <button onClick={() => setSelected(c)}
                    className="flex-1 flex items-center justify-center gap-1.5 py-1.5 text-xs font-semibold text-blue-600 hover:bg-blue-50 rounded-lg transition-colors">
                    <Eye size={12}/> View Profile
                  </button>
                  <button onClick={() => navigate(`/admin/applications?customerId=${c.id}`)}
                    className="flex-1 flex items-center justify-center gap-1.5 py-1.5 text-xs font-semibold text-gray-600 hover:bg-gray-100 rounded-lg transition-colors">
                    Applications
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {totalPages > 1 && (
        <div className="flex items-center justify-between">
          <p className="text-xs text-gray-500">Page {page + 1} of {totalPages}</p>
          <div className="flex gap-2">
            <button disabled={page === 0} onClick={() => setPage(p => p - 1)}
              className="px-4 py-2 text-xs rounded-xl border border-gray-200 disabled:opacity-40 hover:bg-gray-50 font-medium transition-colors">← Prev</button>
            <button disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}
              className="px-4 py-2 text-xs rounded-xl border border-gray-200 disabled:opacity-40 hover:bg-gray-50 font-medium transition-colors">Next →</button>
          </div>
        </div>
      )}

      <CustomerDetailModal customer={selected} onClose={() => setSelected(null)}/>
    </div>
  );
};

export default CustomersPage;
