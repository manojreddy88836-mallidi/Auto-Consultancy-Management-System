import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Car, FileCheck, Search, Banknote, CheckCircle, ChevronRight } from 'lucide-react';

const HomePage = () => {
  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    const handleScroll = () => setScrolled(window.scrollY > 50);
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  return (
    <div className="min-h-screen font-sans bg-[#F8FAFC]">
      {/* Navbar */}
      <nav className={`fixed w-full z-50 transition-all duration-300 ${scrolled ? 'bg-white/90 backdrop-blur-md shadow-sm py-3' : 'bg-transparent py-5'}`}>
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex justify-between items-center">
          <Link to="/" className="flex items-center gap-2">
            <Car className={`h-8 w-8 ${scrolled ? 'text-[#1E88E5]' : 'text-[#1E88E5]'}`} />
            <span className={`text-xl font-bold ${scrolled ? 'text-[#0F1B35]' : 'text-white'}`}>Auto Consultancy</span>
          </Link>
          <div className="hidden md:flex items-center space-x-8">
            <Link to="/about" className={`font-medium hover:text-[#1E88E5] transition-colors ${scrolled ? 'text-gray-600' : 'text-gray-200'}`}>About</Link>
            <Link to="/services" className={`font-medium hover:text-[#1E88E5] transition-colors ${scrolled ? 'text-gray-600' : 'text-gray-200'}`}>Services</Link>
            <Link to="/contact" className={`font-medium hover:text-[#1E88E5] transition-colors ${scrolled ? 'text-gray-600' : 'text-gray-200'}`}>Contact</Link>
            <div className="flex items-center gap-4">
              <Link to="/login" className={`font-medium hover:text-[#1E88E5] transition-colors ${scrolled ? 'text-[#0F1B35]' : 'text-white'}`}>Login</Link>
              <Link to="/register" className="bg-[#1E88E5] hover:bg-[#1976D2] text-white px-5 py-2 rounded-full font-medium transition-colors shadow-sm hover:shadow-md">
                Register
              </Link>
            </div>
          </div>
        </div>
      </nav>

      {/* Hero */}
      <section className="relative pt-32 pb-20 lg:pt-48 lg:pb-32 overflow-hidden bg-gradient-to-br from-[#0F1B35] to-[#1E3A5F]">
        <div className="absolute inset-0 bg-[url('https://images.unsplash.com/photo-1558981806-ec527fa842a9?ixlib=rb-4.0.3&auto=format&fit=crop&w=1920&q=80')] bg-cover bg-center opacity-10 mix-blend-overlay"></div>
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10 text-center">
          <span className="inline-block py-1 px-3 rounded-full bg-blue-500/20 border border-blue-400/30 text-blue-200 text-sm font-semibold tracking-wider mb-6 backdrop-blur-sm">
            🏍️ #1 Bike Finance Consultancy Platform
          </span>
          <h1 className="text-5xl md:text-6xl font-extrabold text-white tracking-tight mb-6 leading-tight">
            Your Trusted Partner in <br className="hidden md:block"/> <span className="text-[#1E88E5]">Bike Finance</span> Consultancy
          </h1>
          <p className="mt-4 text-xl text-blue-100 max-w-2xl mx-auto mb-10">
            Simplifying the process of bike finance, documentation, and loan closures with expert guidance and seamless digital tracking.
          </p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <Link to="/register" className="bg-[#F59E0B] hover:bg-[#D97706] text-white px-8 py-4 rounded-full font-bold text-lg transition-all duration-300 hover:scale-[1.05] shadow-lg hover:shadow-xl">
              Get Started Free
            </Link>
            <Link to="/about" className="bg-transparent border-2 border-white/30 hover:border-white text-white px-8 py-4 rounded-full font-bold text-lg transition-all duration-300 hover:bg-white/10">
              Learn More
            </Link>
          </div>
          
          <div className="mt-20 grid grid-cols-2 md:grid-cols-4 gap-4 md:gap-8 border-t border-white/10 pt-10">
            {[
              { label: 'Customers', value: '500+' },
              { label: 'Bikes Processed', value: '1000+' },
              { label: 'Finance Managed', value: '₹50Cr+' },
              { label: 'Expert Workers', value: '15+' },
            ].map((stat, i) => (
              <div key={i} className="text-center">
                <div className="text-3xl md:text-4xl font-bold text-white mb-1">{stat.value}</div>
                <div className="text-blue-200 text-sm font-medium">{stat.label}</div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Services */}
      <section className="py-20 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16">
            <h2 className="text-3xl md:text-4xl font-bold text-[#0F1B35] mb-4">Our Premium Services</h2>
            <p className="text-gray-600 max-w-2xl mx-auto text-lg">We provide end-to-end solutions for all your bike finance and documentation needs.</p>
          </div>
          
          <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-8">
            {[
              { icon: <FileCheck className="w-8 h-8 text-[#1E88E5]" />, title: 'Document Assistance', desc: 'Expert verification and handling of all required RTO and finance documents.' },
              { icon: <Banknote className="w-8 h-8 text-[#10B981]" />, title: 'Finance Closure', desc: 'Seamless assistance in closing existing loans and obtaining NOCs.' },
              { icon: <Search className="w-8 h-8 text-[#F59E0B]" />, title: 'Status Tracking', desc: 'Real-time updates on your application status through our unified portal.' },
              { icon: <Car className="w-8 h-8 text-[#8B5CF6]" />, title: 'Expert Consultation', desc: 'Get personalized advice on the best finance options for your two-wheeler.' },
            ].map((service, i) => (
              <div key={i} className="bg-gray-50 p-8 rounded-2xl hover:shadow-xl transition-all duration-300 hover:-translate-y-2 border border-gray-100">
                <div className="bg-white w-16 h-16 rounded-xl shadow-sm flex items-center justify-center mb-6">
                  {service.icon}
                </div>
                <h3 className="text-xl font-bold text-[#0F1B35] mb-3">{service.title}</h3>
                <p className="text-gray-600 leading-relaxed">{service.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="py-20 bg-[#0F1B35] relative overflow-hidden">
        <div className="absolute top-0 right-0 -mt-20 -mr-20 w-80 h-80 bg-[#1E88E5] rounded-full opacity-20 blur-3xl"></div>
        <div className="max-w-4xl mx-auto px-4 text-center relative z-10">
          <h2 className="text-3xl md:text-5xl font-bold text-white mb-6">Ready to simplify your bike finance?</h2>
          <p className="text-xl text-gray-300 mb-10">Join hundreds of satisfied customers who have successfully managed their bike documentation with us.</p>
          <Link to="/register" className="inline-flex items-center gap-2 bg-[#F59E0B] hover:bg-[#D97706] text-white px-8 py-4 rounded-full font-bold text-lg transition-all duration-300 hover:scale-[1.05] shadow-lg">
            Create Free Account <ChevronRight size={20} />
          </Link>
        </div>
      </section>

      {/* Footer */}
      <footer className="bg-gray-900 text-gray-400 py-12 border-t border-gray-800">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 grid md:grid-cols-4 gap-8">
          <div className="col-span-2 md:col-span-1">
            <Link to="/" className="flex items-center gap-2 mb-4">
              <Car className="h-6 w-6 text-[#1E88E5]" />
              <span className="text-lg font-bold text-white">Auto Consultancy</span>
            </Link>
            <p className="text-sm">Your trusted partner for seamless bike finance management and documentation.</p>
          </div>
          <div>
            <h4 className="text-white font-semibold mb-4">Quick Links</h4>
            <ul className="space-y-2 text-sm">
              <li><Link to="/about" className="hover:text-white transition-colors">About Us</Link></li>
              <li><Link to="/services" className="hover:text-white transition-colors">Services</Link></li>
              <li><Link to="/contact" className="hover:text-white transition-colors">Contact</Link></li>
            </ul>
          </div>
          <div>
            <h4 className="text-white font-semibold mb-4">Legal</h4>
            <ul className="space-y-2 text-sm">
              <li><a href="#" className="hover:text-white transition-colors">Privacy Policy</a></li>
              <li><a href="#" className="hover:text-white transition-colors">Terms of Service</a></li>
            </ul>
          </div>
          <div>
            <h4 className="text-white font-semibold mb-4">Contact</h4>
            <ul className="space-y-2 text-sm">
              <li>info@autoconsultancy.com</li>
              <li>+91 98765 43210</li>
            </ul>
          </div>
        </div>
        <div className="max-w-7xl mx-auto px-4 mt-12 pt-8 border-t border-gray-800 text-sm text-center">
          &copy; {new Date().getFullYear()} Auto Consultancy. All rights reserved.
        </div>
      </footer>
    </div>
  );
};

export default HomePage;
