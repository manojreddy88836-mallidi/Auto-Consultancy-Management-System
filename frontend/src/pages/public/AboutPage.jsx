import React from 'react';
import { Link } from 'react-router-dom';
import { Rocket, Eye, CheckCircle } from 'lucide-react';

const AboutPage = () => {
  return (
    <div className="min-h-screen bg-gray-50 pt-20">
      <div className="bg-[#0F1B35] py-20">
        <div className="max-w-7xl mx-auto px-4 text-center">
          <h1 className="text-4xl md:text-5xl font-bold text-white mb-4">About Us</h1>
          <p className="text-xl text-blue-100 max-w-2xl mx-auto">We are transforming how bike finance and documentation is managed.</p>
        </div>
      </div>
      
      <div className="max-w-7xl mx-auto px-4 py-16">
        <div className="grid md:grid-cols-2 gap-12 items-center mb-20">
          <div>
            <h2 className="text-3xl font-bold text-[#0F1B35] mb-6">Our Story</h2>
            <div className="w-16 h-1 bg-[#1E88E5] mb-6"></div>
            <p className="text-gray-600 mb-4 leading-relaxed">
              Founded in 2020, Auto Consultancy emerged from a simple observation: navigating the complexities of bike finance, RC transfers, and loan NOCs was overly complicated for the average consumer.
            </p>
            <p className="text-gray-600 leading-relaxed">
              We built a unified platform that connects customers, our expert workers, and financial institutions to ensure a seamless, transparent, and quick process. Today, we manage thousands of applications with a 98% success rate.
            </p>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
              <Rocket className="w-10 h-10 text-[#1E88E5] mb-4" />
              <h3 className="font-bold text-lg mb-2">Our Mission</h3>
              <p className="text-sm text-gray-600">To simplify vehicle documentation and finance for everyone.</p>
            </div>
            <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 mt-8">
              <Eye className="w-10 h-10 text-[#F59E0B] mb-4" />
              <h3 className="font-bold text-lg mb-2">Our Vision</h3>
              <p className="text-sm text-gray-600">To be India's most trusted auto consultancy platform.</p>
            </div>
          </div>
        </div>

        <div className="mb-20">
          <h2 className="text-3xl font-bold text-center text-[#0F1B35] mb-12">Our Leadership Team</h2>
          <div className="grid md:grid-cols-3 gap-8">
            {[
              { name: 'Rahul Sharma', role: 'CEO & Founder', initials: 'RS', color: 'bg-blue-500' },
              { name: 'Priya Patel', role: 'Operations Head', initials: 'PP', color: 'bg-green-500' },
              { name: 'Amit Kumar', role: 'Finance Director', initials: 'AK', color: 'bg-purple-500' }
            ].map((member, i) => (
              <div key={i} className="bg-white rounded-xl shadow-sm overflow-hidden text-center hover:shadow-md transition-shadow">
                <div className={`h-32 ${member.color} flex items-center justify-center`}>
                  <div className="w-24 h-24 bg-white rounded-full flex items-center justify-center text-3xl font-bold text-gray-800 border-4 border-white shadow-lg translate-y-12">
                    {member.initials}
                  </div>
                </div>
                <div className="pt-16 pb-8 px-6">
                  <h3 className="text-xl font-bold text-gray-900">{member.name}</h3>
                  <p className="text-[#1E88E5] font-medium mt-1">{member.role}</p>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="bg-white rounded-2xl p-10 border border-gray-100 shadow-sm text-center">
          <h2 className="text-3xl font-bold text-[#0F1B35] mb-4">Ready to get started?</h2>
          <p className="text-gray-600 mb-8 max-w-2xl mx-auto">Join 500+ satisfied customers who have experienced hassle-free bike finance management.</p>
          <Link to="/register" className="inline-block bg-[#1E88E5] hover:bg-[#1976D2] text-white px-8 py-3 rounded-full font-bold transition-transform hover:scale-[1.02]">
            Create an Account Now
          </Link>
        </div>
      </div>
    </div>
  );
};

export default AboutPage;
