import React, { useState, useEffect } from 'react';
import { toast } from 'react-hot-toast';
import { User, Phone, Mail, MapPin, Calendar, Shield, CheckCircle, Edit3, Save, X } from 'lucide-react';
import useAuth from '../../hooks/useAuth';
import { getCustomerProfile as getMyProfile, updateCustomerProfile as updateMyProfile } from '../../api/customerApi';

const STATES = [
  'Andhra Pradesh','Arunachal Pradesh','Assam','Bihar','Chhattisgarh','Goa','Gujarat',
  'Haryana','Himachal Pradesh','Jharkhand','Karnataka','Kerala','Madhya Pradesh',
  'Maharashtra','Manipur','Meghalaya','Mizoram','Nagaland','Odisha','Punjab',
  'Rajasthan','Sikkim','Tamil Nadu','Telangana','Tripura','Uttar Pradesh',
  'Uttarakhand','West Bengal','Delhi','Jammu & Kashmir','Ladakh'
];

const ID_TYPES = ['AADHAAR','PAN','VOTER_ID','PASSPORT','DRIVING_LICENSE'];

const Field = ({ label, children, icon: Icon }) => (
  <div>
    <label className="block text-xs font-bold text-gray-500 uppercase tracking-wide mb-1.5">
      {Icon && <Icon size={12} className="inline mr-1 opacity-60"/>}{label}
    </label>
    {children}
  </div>
);

const inputCls = "w-full px-4 py-2.5 rounded-xl border border-gray-200 text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none transition-all";
const readCls  = "w-full px-4 py-2.5 rounded-xl border border-gray-100 bg-gray-50 text-sm text-gray-500 cursor-not-allowed";

const ProfilePage = () => {
  const { user, refreshUser } = useAuth();
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(false);
  const [saving,  setSaving]  = useState(false);
  const [form, setForm]       = useState({});

  useEffect(() => {
    getMyProfile()
      .then(res => {
        const d = res.data?.data || res.data;
        setProfile(d);
        setForm({
          firstName:           d.user?.firstName || user?.firstName || '',
          lastName:            d.user?.lastName  || user?.lastName  || '',
          phone:               d.user?.phone     || user?.phone     || '',
          dateOfBirth:         d.dateOfBirth     || '',
          address:             d.address         || '',
          city:                d.city            || '',
          state:               d.state           || '',
          pincode:             d.pincode         || '',
          identityProof:       d.identityProof   || '',
          identityProofNumber: d.identityProofNumber || '',
        });
      })
      .catch(() => {
        // Pre-fill from auth context if API fails
        setForm({
          firstName: user?.firstName || '', lastName: user?.lastName || '',
          phone: user?.phone || '', dateOfBirth: '', address: '',
          city: '', state: '', pincode: '', identityProof: '', identityProofNumber: '',
        });
      })
      .finally(() => setLoading(false));
  }, [user]);

  const handleSave = async (e) => {
    e.preventDefault();
    if (!form.firstName) { toast.error('First name is required'); return; }
    setSaving(true);
    try {
      await updateMyProfile(form);
      toast.success('Profile updated successfully!');
      if (refreshUser) await refreshUser();
      setEditing(false);
      // Refresh profile
      const res = await getMyProfile();
      setProfile(res.data?.data || res.data);
    } catch (err) {
      toast.error(err?.response?.data?.message || 'Failed to update profile');
    } finally { setSaving(false); }
  };

  const cancelEdit = () => {
    setEditing(false);
    if (profile) {
      setForm({
        firstName: profile.user?.firstName || user?.firstName || '',
        lastName:  profile.user?.lastName  || user?.lastName  || '',
        phone:     profile.user?.phone     || user?.phone     || '',
        dateOfBirth:         profile.dateOfBirth || '',
        address:             profile.address     || '',
        city:                profile.city        || '',
        state:               profile.state       || '',
        pincode:             profile.pincode     || '',
        identityProof:       profile.identityProof       || '',
        identityProofNumber: profile.identityProofNumber || '',
      });
    }
  };

  const f = (key) => form[key] || '';
  const set = (key) => (e) => setForm(p => ({ ...p, [key]: e.target.value }));

  const isComplete = profile?.profileComplete;
  const displayName = `${f('firstName')} ${f('lastName')}`.trim() || user?.email;

  if (loading) return (
    <div className="flex items-center justify-center h-64">
      <div className="w-8 h-8 border-3 border-[#1E88E5] border-t-transparent rounded-full animate-spin"/>
    </div>
  );

  return (
    <div className="max-w-3xl mx-auto space-y-5">
      {/* Profile Card Header */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
        <div className="h-28 bg-gradient-to-r from-[#0F1B35] via-[#1E3A5F] to-[#1E88E5] relative">
          <div className="absolute inset-0 opacity-20"
            style={{ backgroundImage:'radial-gradient(circle at 70% 50%, white 1px, transparent 1px)', backgroundSize:'24px 24px' }}/>
        </div>
        <div className="px-6 pb-6">
          <div className="flex flex-col sm:flex-row sm:items-end sm:justify-between gap-4 -mt-12 mb-4">
            <div className="flex items-end gap-4">
              <div className="w-24 h-24 rounded-2xl bg-gradient-to-br from-[#0F1B35] to-[#1E88E5] flex items-center justify-center text-white text-3xl font-bold border-4 border-white shadow-xl flex-shrink-0">
                {(f('firstName')[0] || '?').toUpperCase()}{(f('lastName')[0] || '').toUpperCase()}
              </div>
              <div className="pb-1">
                <h1 className="text-xl font-bold text-gray-900">{displayName}</h1>
                <p className="text-sm text-gray-400">{user?.email}</p>
                <div className="flex items-center gap-2 mt-1.5">
                  <span className="flex items-center gap-1 text-xs font-semibold text-blue-700 bg-blue-100 px-2.5 py-0.5 rounded-full">
                    <Shield size={10}/> Customer
                  </span>
                  <span className={`flex items-center gap-1 text-xs font-semibold px-2.5 py-0.5 rounded-full ${
                    isComplete ? 'bg-green-100 text-green-700' : 'bg-amber-100 text-amber-700'
                  }`}>
                    <CheckCircle size={10}/>
                    {isComplete ? 'Profile Complete' : 'Profile Incomplete'}
                  </span>
                </div>
              </div>
            </div>
            <div className="flex gap-2 sm:pb-1">
              {!editing ? (
                <button onClick={() => setEditing(true)}
                  className="flex items-center gap-2 px-4 py-2 bg-[#1E88E5] hover:bg-[#1976D2] text-white rounded-xl text-sm font-semibold transition-all shadow-md">
                  <Edit3 size={15}/> Edit Profile
                </button>
              ) : (
                <button onClick={cancelEdit}
                  className="flex items-center gap-2 px-4 py-2 border border-gray-200 hover:bg-gray-50 text-gray-600 rounded-xl text-sm font-medium transition-all">
                  <X size={15}/> Cancel
                </button>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Form */}
      <form onSubmit={handleSave} className="space-y-5">
        {/* Personal Info */}
        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6">
          <h2 className="text-sm font-bold text-gray-900 uppercase tracking-wide mb-5 flex items-center gap-2">
            <User size={15} className="text-[#1E88E5]"/> Personal Information
          </h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Field label="First Name" icon={User}>
              {editing
                ? <input value={f('firstName')} onChange={set('firstName')} required placeholder="First name" className={inputCls}/>
                : <p className={readCls}>{f('firstName') || '—'}</p>}
            </Field>
            <Field label="Last Name">
              {editing
                ? <input value={f('lastName')} onChange={set('lastName')} placeholder="Last name" className={inputCls}/>
                : <p className={readCls}>{f('lastName') || '—'}</p>}
            </Field>
            <Field label="Email" icon={Mail}>
              <input disabled value={user?.email || ''} className={readCls}/>
              {editing && <p className="text-xs text-gray-400 mt-1">Email cannot be changed</p>}
            </Field>
            <Field label="Phone" icon={Phone}>
              {editing
                ? <input value={f('phone')} onChange={set('phone')} placeholder="10-digit mobile" maxLength={10} className={inputCls}/>
                : <p className={readCls}>{f('phone') || '—'}</p>}
            </Field>
            <Field label="Date of Birth" icon={Calendar}>
              {editing
                ? <input type="date" value={f('dateOfBirth')} onChange={set('dateOfBirth')} max={new Date().toISOString().split('T')[0]} className={inputCls}/>
                : <p className={readCls}>{f('dateOfBirth') || '—'}</p>}
            </Field>
          </div>
        </div>

        {/* Address */}
        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6">
          <h2 className="text-sm font-bold text-gray-900 uppercase tracking-wide mb-5 flex items-center gap-2">
            <MapPin size={15} className="text-[#1E88E5]"/> Address Details
          </h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Field label="Full Address">
              <div className="sm:col-span-2">
                {editing
                  ? <textarea value={f('address')} onChange={set('address')} rows={2} placeholder="House/Flat, Street, Area" className={`${inputCls} resize-none`}/>
                  : <p className={readCls}>{f('address') || '—'}</p>}
              </div>
            </Field>
            <Field label="City">
              {editing
                ? <input value={f('city')} onChange={set('city')} placeholder="City" className={inputCls}/>
                : <p className={readCls}>{f('city') || '—'}</p>}
            </Field>
            <Field label="State">
              {editing
                ? (
                  <select value={f('state')} onChange={set('state')} className={inputCls}>
                    <option value="">Select state</option>
                    {STATES.map(s => <option key={s} value={s}>{s}</option>)}
                  </select>
                )
                : <p className={readCls}>{f('state') || '—'}</p>}
            </Field>
            <Field label="Pincode">
              {editing
                ? <input value={f('pincode')} onChange={set('pincode')} placeholder="6-digit pincode" maxLength={6} className={inputCls}/>
                : <p className={readCls}>{f('pincode') || '—'}</p>}
            </Field>
          </div>
        </div>

        {/* Identity */}
        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6">
          <h2 className="text-sm font-bold text-gray-900 uppercase tracking-wide mb-5 flex items-center gap-2">
            <Shield size={15} className="text-[#1E88E5]"/> Identity Verification
          </h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Field label="ID Proof Type">
              {editing
                ? (
                  <select value={f('identityProof')} onChange={set('identityProof')} className={inputCls}>
                    <option value="">Select ID type</option>
                    {ID_TYPES.map(t => <option key={t} value={t}>{t.replace(/_/g,' ')}</option>)}
                  </select>
                )
                : <p className={readCls}>{f('identityProof')?.replace(/_/g,' ') || '—'}</p>}
            </Field>
            <Field label="ID Number">
              {editing
                ? <input value={f('identityProofNumber')} onChange={set('identityProofNumber')} placeholder="e.g. XXXX-XXXX-XXXX" className={inputCls}/>
                : <p className={`${readCls} tracking-wider`}>{f('identityProofNumber') || '—'}</p>}
            </Field>
          </div>
        </div>

        {/* Save Button */}
        {editing && (
          <div className="flex justify-end gap-3 pb-4">
            <button type="button" onClick={cancelEdit}
              className="px-6 py-2.5 rounded-xl border border-gray-200 text-sm font-medium hover:bg-gray-50 transition-colors">
              Cancel
            </button>
            <button type="submit" disabled={saving}
              className="flex items-center gap-2 px-6 py-2.5 rounded-xl bg-[#1E88E5] hover:bg-[#1976D2] text-white text-sm font-bold transition-all disabled:opacity-60 shadow-md hover:shadow-lg">
              {saving ? 'Saving…' : <><Save size={15}/> Save Changes</>}
            </button>
          </div>
        )}
      </form>
    </div>
  );
};

export default ProfilePage;
