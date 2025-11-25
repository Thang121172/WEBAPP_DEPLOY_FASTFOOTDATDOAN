type Sender = (lat: number, lng: number) => Promise<void>

export function createThrottledSender(sendFn: Sender, minIntervalSeconds = 5) {
  let lastSent = 0
  let pending: ReturnType<typeof setTimeout> | null = null

  return function send(lat: number, lng: number) {
    const now = Date.now() / 1000
    const diff = now - lastSent
    if (diff >= minIntervalSeconds) {
      lastSent = now
      sendFn(lat, lng).catch(console.error)
      return
    }
    if (pending) return
    const wait = Math.max(0, Math.ceil((minIntervalSeconds - diff) * 1000))
    pending = setTimeout(() => {
      lastSent = Date.now() / 1000
      pending = null
      sendFn(lat, lng).catch(console.error)
    }, wait)
  }
}

export async function startShipperTracking(sendFn: Sender, minIntervalSeconds = 5) {
  if (!('geolocation' in navigator)) throw new Error('Geolocation not supported')
  const sender = createThrottledSender(sendFn, minIntervalSeconds)
  const watchId = navigator.geolocation.watchPosition((pos) => {
    sender(pos.coords.latitude, pos.coords.longitude)
  }, (err) => console.error('geo err', err), { enableHighAccuracy: true })
  return () => navigator.geolocation.clearWatch(watchId)
}
