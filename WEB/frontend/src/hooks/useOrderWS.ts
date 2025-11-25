// useOrderWS placeholder

import { useEffect } from 'react'

export function useOrderWS(orderId: number | null) {
  useEffect(() => {
    if (!orderId) return
    // connect to websocket
  }, [orderId])
}
