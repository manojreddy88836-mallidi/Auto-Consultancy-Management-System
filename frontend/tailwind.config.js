/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        primary: { DEFAULT: '#0F1B35', 50: '#E8ECF4', 100: '#D0D9E9', 500: '#1E3A5F', 700: '#0F1B35', 900: '#070D1A' },
        accent: { DEFAULT: '#1E88E5', 50: '#E3F2FD', 100: '#BBDEFB', 500: '#1E88E5', 700: '#1565C0' },
        gold: { DEFAULT: '#F59E0B', 50: '#FFFBEB', 500: '#F59E0B', 700: '#B45309' },
      },
      fontFamily: { sans: ['Inter', 'sans-serif'] },
    },
  },
  plugins: [],
}
