import { useState, useEffect } from 'react'

export function useLocationConsent() {
  const [granted, setGranted] = useState<boolean | null>(null)

  useEffect(() => {
    if (!('permissions' in navigator)) return
    // @ts-ignore
    navigator.permissions.query({ name: 'geolocation' }).then((status: any) => {
      setGranted(status.state === 'granted')
      status.onchange = () => setGranted(status.state === 'granted')
    })
  }, [])

  return { granted }
}
