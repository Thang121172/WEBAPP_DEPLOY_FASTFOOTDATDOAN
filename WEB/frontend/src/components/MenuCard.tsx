import React from 'react'

type Props = {
  id: number
  name: string
  price?: number
  stock?: number
  discount?: number
}

export default function MenuCard({ id, name, price, stock, discount }: Props){
  return (
    <article className="card relative overflow-hidden bg-white rounded-xl shadow-lg hover:shadow-xl transition duration-300 cursor-pointer">
      {/* image placeholder - Dùng grabGreen-100 */}
      <div className="w-full h-36 bg-grabGreen-100 flex items-center justify-center rounded-t-xl">
        {/* Biểu tượng placeholder */}
        <svg xmlns="http://www.w3.org/2000/svg" className="h-12 w-12 text-grabGreen-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z" />
        </svg>
      </div>
      {/* discount badge - Dùng grabGreen-700 */}
      {discount ? (
        <div className="absolute left-3 top-3 bg-grabGreen-700 text-white text-xs font-semibold px-3 py-1 rounded-full shadow-md">
          -{discount}%
        </div>
      ) : null}
      <div className="p-4">
        <h3 className="font-bold text-lg text-gray-900 truncate">{name}</h3>
        <p className="text-sm text-gray-500 mt-1">Địa chỉ • 2.1 km</p>
        <div className="mt-3 flex items-center gap-2">
          {/* Nút "Yêu thích" giữ màu trung tính */}
          <button className="text-xs px-3 py-1 border border-gray-300 text-gray-600 rounded-full bg-gray-50 hover:bg-gray-100 transition duration-150">
            Yêu thích
          </button>
          {/* Nút "Mở cửa" dùng viền và chữ Grab Green */}
          <button className="text-xs px-3 py-1 border-2 border-grabGreen-700 text-grabGreen-700 rounded-full bg-grabGreen-50 font-medium">
            Mở cửa
          </button>
        </div>
      </div>
    </article>
  )
}
