export const APPLICATION_STATUSES = [
  { value: 'DRAFT', label: 'Draft', color: 'gray' },
  { value: 'SUBMITTED', label: 'Submitted', color: 'blue' },
  { value: 'UNDER_REVIEW', label: 'Under Review', color: 'yellow' },
  { value: 'DOCUMENT_VERIFICATION', label: 'Document Verification', color: 'orange' },
  { value: 'FINANCE_VERIFICATION', label: 'Finance Verification', color: 'purple' },
  { value: 'WORKER_ASSIGNED', label: 'Worker Assigned', color: 'indigo' },
  { value: 'APPROVED', label: 'Approved', color: 'green' },
  { value: 'REJECTED', label: 'Rejected', color: 'red' },
  { value: 'COMPLETED', label: 'Completed', color: 'teal' },
];

export const DOCUMENT_TYPES = [
  { value: 'AADHAAR', label: 'Aadhaar / Identity Proof' },
  { value: 'PAN_CARD', label: 'PAN Card' },
  { value: 'DRIVING_LICENCE', label: 'Driving Licence' },
  { value: 'REGISTRATION_CERTIFICATE', label: 'Registration Certificate (RC)' },
  { value: 'INSURANCE', label: 'Insurance Document' },
  { value: 'FINANCE_DOCUMENTS', label: 'Finance Documents' },
  { value: 'LOAN_STATEMENT', label: 'Loan Statement' },
  { value: 'NOC', label: 'NOC Certificate' },
  { value: 'ADDRESS_PROOF', label: 'Address Proof' },
  { value: 'OTHER', label: 'Other Documents' },
];

export const BIKE_CONDITIONS = ['Excellent', 'Good', 'Fair', 'Poor'];
export const FUEL_TYPES = ['Petrol', 'Electric', 'Hybrid', 'CNG'];
export const VEHICLE_TYPES = ['Motorcycle', 'Scooter', 'Moped'];
export const BIKE_CATEGORIES = ['Commuter', 'Sports', 'Cruiser', 'Scooter', 'Electric', 'Adventure', 'Touring', 'Naked'];
export const IDENTITY_PROOF_TYPES = ['Aadhaar Card', 'PAN Card', 'Driving Licence', 'Passport', 'Voter ID'];
export const INDIAN_STATES = [
  'Andhra Pradesh','Arunachal Pradesh','Assam','Bihar','Chhattisgarh','Goa','Gujarat','Haryana',
  'Himachal Pradesh','Jharkhand','Karnataka','Kerala','Madhya Pradesh','Maharashtra','Manipur',
  'Meghalaya','Mizoram','Nagaland','Odisha','Punjab','Rajasthan','Sikkim','Tamil Nadu','Telangana',
  'Tripura','Uttar Pradesh','Uttarakhand','West Bengal','Andaman and Nicobar Islands','Chandigarh',
  'Delhi','Jammu and Kashmir','Ladakh','Lakshadweep','Puducherry'
];

export const PAYMENT_METHODS = ['Cash', 'Bank Transfer', 'Cheque', 'Online Payment'];
export const EMI_STATUSES = ['Regular', 'Overdue', 'Partial'];
export const LOAN_CLOSURE_STATUSES = ['Active', 'Completed', 'Closed'];

export const FINANCE_STATUSES = [
  { value: 'ACTIVE', label: 'Active Finance', color: 'blue' },
  { value: 'COMPLETED', label: 'Finance Completed', color: 'green' },
  { value: 'CLOSED', label: 'Loan Closed', color: 'teal' },
  { value: 'NOC_PENDING', label: 'NOC Pending', color: 'yellow' },
  { value: 'NO_FINANCE', label: 'No Finance', color: 'gray' },
];
