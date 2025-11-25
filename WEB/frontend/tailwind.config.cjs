/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // Định nghĩa màu Xanh Grab
        'grabGreen': {
          DEFAULT: '#00A651', // Base color (tương đương ~700)
          '50': '#F0FDF4', // Rất nhạt, dùng làm nền banner
          '100': '#DCFCE7',
          '200': '#BBF7D0',
          '300': '#86EFAC',
          '400': '#4ADE80',
          '500': '#10B981', // Màu xanh Grab tươi hơn
          '600': '#059669',
          '700': '#047857', // Màu xanh đậm chính
          '800': '#065F46',
          '900': '#064E3B',
          '950': '#022C22',
        },
      },
      fontFamily: {
        sans: ['Inter', 'sans-serif'],
      },
    },
  },
  plugins: [],
}
