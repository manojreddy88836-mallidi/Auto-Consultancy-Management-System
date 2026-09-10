import React, { Suspense, lazy } from 'react';
import { Routes, Route } from 'react-router-dom';
import ProtectedRoute from './routes/ProtectedRoute';

// Layouts (eager - small, always needed)
import AdminLayout   from './components/layout/AdminLayout';
import WorkerLayout  from './components/layout/WorkerLayout';
import CustomerLayout from './components/layout/CustomerLayout';

// Public pages (lazy)
const HomePage          = lazy(() => import('./pages/public/HomePage'));
const AboutPage         = lazy(() => import('./pages/public/AboutPage'));
const ServicesPage      = lazy(() => import('./pages/public/ServicesPage'));
const ContactPage       = lazy(() => import('./pages/public/ContactPage'));
const EmiCalculatorPage = lazy(() => import('./pages/public/EmiCalculatorPage'));
const LoginPage         = lazy(() => import('./pages/auth/LoginPage'));
const RegisterPage      = lazy(() => import('./pages/auth/RegisterPage'));
const NotFoundPage      = lazy(() => import('./pages/public/NotFoundPage'));

// Admin pages (lazy)
const AdminDashboard           = lazy(() => import('./pages/admin/AdminDashboard'));
const CustomersPage            = lazy(() => import('./pages/admin/CustomersPage'));
const WorkersPage              = lazy(() => import('./pages/admin/WorkersPage'));
const ApplicationsPage         = lazy(() => import('./pages/admin/ApplicationsPage'));
const AdminApplicationDetailPage = lazy(() => import('./pages/admin/ApplicationDetailPage'));
const BrandsPage               = lazy(() => import('./pages/admin/BrandsPage'));
const BikeModelsPage           = lazy(() => import('./pages/admin/BikeModelsPage'));
const ManufacturersPage        = lazy(() => import('./pages/admin/ManufacturersPage'));
const ManufacturingYearsPage   = lazy(() => import('./pages/admin/ManufacturingYearsPage'));
const FinanceRecordsPage       = lazy(() => import('./pages/admin/FinanceRecordsPage'));
const AdminDocumentsPage       = lazy(() => import('./pages/admin/DocumentsPage'));
const ReportsPage              = lazy(() => import('./pages/admin/ReportsPage'));
const BikesPage                = lazy(() => import('./pages/admin/BikesPage'));
const AdminOffersPage          = lazy(() => import('./pages/admin/OffersPage'));
const WorkerTasksPage          = lazy(() => import('./pages/admin/WorkerTasksPage'));
const CreateWorkerTaskPage     = lazy(() => import('./pages/admin/CreateWorkerTaskPage'));
const WorkerTaskDetailPage     = lazy(() => import('./pages/admin/WorkerTaskDetailPage'));

// Worker pages (lazy)
const WorkerDashboard          = lazy(() => import('./pages/worker/WorkerDashboard'));
const MyTasksPage              = lazy(() => import('./pages/worker/MyTasksPage'));
const TaskDetailPage           = lazy(() => import('./pages/worker/TaskDetailPage'));
const ServiceJobsPage          = lazy(() => import('./pages/worker/ServiceJobsPage'));
const CollectionsPage          = lazy(() => import('./pages/worker/CollectionsPage'));
const CustomerVisitsPage       = lazy(() => import('./pages/worker/CustomerVisitsPage'));
const BikeRecoveryPage         = lazy(() => import('./pages/worker/BikeRecoveryPage'));
const AssignedApplicationsPage = lazy(() => import('./pages/worker/AssignedApplicationsPage'));
const WorkerApplicationDetailPage = lazy(() => import('./pages/worker/WorkerApplicationDetailPage'));
const DocumentVerificationPage = lazy(() => import('./pages/worker/DocumentVerificationPage'));
const WorkerOffersPage         = lazy(() => import('./pages/worker/OffersPage'));

// Customer pages (lazy)
const CustomerDashboard        = lazy(() => import('./pages/customer/CustomerDashboard'));
const ProfilePage              = lazy(() => import('./pages/customer/ProfilePage'));
const MyApplicationsPage       = lazy(() => import('./pages/customer/MyApplicationsPage'));
const SubmitBikeDetailsPage    = lazy(() => import('./pages/customer/SubmitBikeDetailsPage'));
const ApplicationStatusPage    = lazy(() => import('./pages/customer/ApplicationStatusPage'));
const CustomerDocumentsPage    = lazy(() => import('./pages/customer/DocumentsPage'));
const SaleBikesPage            = lazy(() => import('./pages/customer/SaleBikesPage'));
const BikeDetailPage           = lazy(() => import('./pages/customer/BikeDetailPage'));
const MyOffersPage             = lazy(() => import('./pages/customer/MyOffersPage'));

const PageLoader = () => (
  <div className="flex items-center justify-center min-h-screen bg-[#F8FAFC]">
    <div className="w-8 h-8 border-2 border-[#1E88E5] border-t-transparent rounded-full animate-spin"/>
  </div>
);

const App = () => (
  <Suspense fallback={<PageLoader />}>
    <Routes>
      <Route path="/"               element={<HomePage />} />
      <Route path="/about"          element={<AboutPage />} />
      <Route path="/services"       element={<ServicesPage />} />
      <Route path="/contact"        element={<ContactPage />} />
      <Route path="/login"          element={<LoginPage />} />
      <Route path="/register"       element={<RegisterPage />} />
      <Route path="/emi-calculator" element={<EmiCalculatorPage />} />

      <Route element={<ProtectedRoute allowedRoles={['ADMIN']} />}>
        <Route path="/admin" element={<AdminLayout />}>
          <Route index                        element={<AdminDashboard />} />
          <Route path="customers"             element={<CustomersPage />} />
          <Route path="workers"               element={<WorkersPage />} />
          <Route path="applications"          element={<ApplicationsPage />} />
          <Route path="applications/:id"      element={<AdminApplicationDetailPage />} />
          <Route path="brands"                element={<BrandsPage />} />
          <Route path="manufacturers"         element={<ManufacturersPage />} />
          <Route path="bike-models"           element={<BikeModelsPage />} />
          <Route path="bikes"                 element={<BikesPage />} />
          <Route path="manufacturing-years"   element={<ManufacturingYearsPage />} />
          <Route path="finance-records"       element={<FinanceRecordsPage />} />
          <Route path="documents"             element={<AdminDocumentsPage />} />
          <Route path="reports"               element={<ReportsPage />} />
          <Route path="offers"                element={<AdminOffersPage />} />
          <Route path="worker-tasks"          element={<WorkerTasksPage />} />
          <Route path="worker-tasks/create"   element={<CreateWorkerTaskPage />} />
          <Route path="worker-tasks/:id"      element={<WorkerTaskDetailPage />} />
        </Route>
      </Route>

      <Route element={<ProtectedRoute allowedRoles={['WORKER']} />}>
        <Route path="/worker" element={<WorkerLayout />}>
          <Route index                        element={<WorkerDashboard />} />
          <Route path="tasks"                 element={<MyTasksPage />} />
          <Route path="tasks/:id"             element={<TaskDetailPage />} />
          <Route path="service"               element={<ServiceJobsPage />} />
          <Route path="collections"           element={<CollectionsPage />} />
          <Route path="visits"                element={<CustomerVisitsPage />} />
          <Route path="recovery"              element={<BikeRecoveryPage />} />
          <Route path="applications"          element={<AssignedApplicationsPage />} />
          <Route path="applications/:id"      element={<WorkerApplicationDetailPage />} />
          <Route path="documents"             element={<DocumentVerificationPage />} />
          <Route path="bikes"                 element={<BikesPage />} />
          <Route path="offers"                element={<WorkerOffersPage />} />
        </Route>
      </Route>

      <Route element={<ProtectedRoute allowedRoles={['CUSTOMER']} />}>
        <Route path="/customer" element={<CustomerLayout />}>
          <Route index                        element={<CustomerDashboard />} />
          <Route path="profile"               element={<ProfilePage />} />
          <Route path="applications"          element={<MyApplicationsPage />} />
          <Route path="submit"                element={<SubmitBikeDetailsPage />} />
          <Route path="applications/:id"      element={<ApplicationStatusPage />} />
          <Route path="documents"             element={<CustomerDocumentsPage />} />
          <Route path="bikes"                 element={<SaleBikesPage />} />
          <Route path="bikes/:id"             element={<BikeDetailPage />} />
          <Route path="offers"                element={<MyOffersPage />} />
        </Route>
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  </Suspense>
);

export default App;