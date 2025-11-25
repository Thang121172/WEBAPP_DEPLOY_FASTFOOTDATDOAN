import React, { useEffect, useState } from 'react'
import api from '../services/http'

export default function MerchantApp(){
  const [menus, setMenus] = useState<any[]>([])
  useEffect(()=>{
    api.get('/merchant/').then(r=>setMenus(r.data)).catch(()=>{})
  },[])
  const updateStock = (id:number)=>{
  api.post(`/merchant/${id}/update_stock/`, {stock: 10})
  }
  return (
    <div>
      <h2>Merchant</h2>
      <ul>
        {menus.map(m => <li key={m.id}>{m.name} - stock: {m.stock} <button onClick={()=>updateStock(m.id)}>+10</button></li>)}
      </ul>
    </div>
  )
}
