import React from 'react';
import { FileText, Car, Clock, ShieldCheck, Banknote, Users, CheckCircle } from 'lucide-react';
import { Link } from 'react-router-dom';

const ServicesPage = () => {
  const services = [
    {
      title: 'Bike Finance Consultancy',
      icon: <Banknote className="w-12 h-12 text-[#1E88E5]" />,
      description: 'End-to-end assistance in securing the best two-wheeler loans with competitive interest rates.',
      features: ['Loan eligibility check', 'Interest rate comparison', 'Bank coordination', 'Quick disbursement']
    },
    {
      title: 'Document Assistance',
      icon: <FileText className="w-12 h-12 text-[#10B981]" />,
      description: 'Comprehensive help with RC transfers, hypothecation additions, and RTO paperwork.',
      features: ['RC Transfer (Ownership)', 'HP Addition', 'HP Deletion', 'Duplicate RC']
    },
    {
      title: 'Application Status Tracking',
      icon: <Clock className="w-12 h-12 text-[#F59E0B]" />,
      description: 'Real-time updates on your finance and RTO application status through our online portal.',
      features: ['Live tracking dashboard', 'SMS/Email alerts', 'Milestone updates', 'Dedicated support']
    },
    {
      title: 'Loan NOC Assistance',
      icon: <ShieldCheck className="w-12 h-12 text-[#8B5CF6]" />,
      description: 'Hassle-free process to obtain No Objection Certificates from banks after loan closure.',
      features: ['Bank liaisoning', 'NOC procurement', 'RTO submission', 'Form 35 processing']
    },
    {
      title: 'Finance Closure Help',
      icon: <CheckCircle className="w-12 h-12 text-[#EC4899]" />,
      description: 'Guidance and processing for foreclosing your existing two-wheeler loans.',
      features: ['Foreclosure calculation', 'Payment assistance', 'Bank coordination', 'Closure letter']
    },
    {
      title: 'Expert Consultation',
      icon: <Users className="w-12 h-12 text-[#06B6D4]" />,
      description: 'Personalized advice from our experienced auto finance consultants.',
      features: ['One-on-one sessions', 'Financial planning', 'Market insights', 'Credit score guidance']
    }
  ];

  return (
    <div className="min-h-screen bg-gray-50 pt-20">
      <div className="bg-[#0F1B35] py-20">
        <div className="max-w-7xl mx-auto px-4 text-center">
          <h1 className="text-4xl md:text-5xl font-bold text-white mb-4">Our Services</h1>
          <p className="text-xl text-blue-100 max-w-2xl mx-auto">Comprehensive solutions for your bike finance and documentation needs.</p>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 py-16">
        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-8 mb-20">
          {services.map((service, index) => (
            <div key={index} className="bg-white rounded-xl shadow-sm hover:shadow-md transition-all duration-300 hover:-translate-y-1 p-8 border border-gray-100">
              <div className="mb-6">{service.icon}</div>
              <h3 className="text-2xl font-bold text-gray-900 mb-3">{service.title}</h3>
              <p className="text-gray-600 mb-6">{service.description}</p>
              <ul className="space-y-2">
                {service.features.map((feature, i) => (
                  <li key={i} className="flex items-start gap-2 text-sm text-gray-700">
                    <CheckCircle className="w-5 h-5 text-green-500 shrink-0" />
                    <span>{feature}</span>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        <div className="bg-white rounded-2xl p-10 border border-gray-100 shadow-sm mb-20">
          <h2 className="text-3xl font-bold text-center text-[#0F1B35] mb-12">How It Works</h2>
          <div className="grid md:grid-cols-4 gap-8 text-center relative">
            <div className="hidden md:block absolute top-12 left-[12%] right-[12%] h-0.5 bg-blue-100"></div>
            {[
              { step: '1', title: 'Register', desc: 'Create your free account on our platform' },
              { step: '2', title: 'Submit Details', desc: 'Provide bike and basic finance info' },
              { step: '3', title: 'Verification', desc: 'Our experts verify your documents' },
              { step: '4', title: 'Completion', desc: 'Process completed successfully' }
            ].map((item, i) => (
              <div key={i} className="relative z-10">
                <div className="w-24 h-24 bg-white rounded-full mx-auto border-4 border-[#1E88E5] flex items-center justify-center text-3xl font-bold text-[#1E88E5] mb-6 shadow-sm">
                  {item.step}
                </div>
                <h3 className="text-xl font-bold text-gray-900 mb-2">{item.title}</h3>
                <p className="text-gray-600 text-sm">{item.desc}</p>
              </div>
            ))}
          </div>
        </div>

        <div className="bg-[#1E88E5] rounded-2xl p-10 text-center text-white shadow-lg">
          <h2 className="text-3xl font-bold mb-4">Need personalized assistance?</h2>
          <p className="text-blue-100 mb-8 max-w-2xl mx-auto text-lg">Our experts are ready to help you navigate through your specific requirements.</p>
          <Link to="/contact" className="inline-block bg-white text-[#1E88E5] px-8 py-3 rounded-full font-bold transition-transform hover:scale-[1.02] shadow-sm hover:shadow-md">
            Contact Us Today
          </Link>
        </div>
      </div>
    </div>
  );
};

export default ServicesPage;
