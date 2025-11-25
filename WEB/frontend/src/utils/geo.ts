export function toRad(deg: number) { return deg * Math.PI / 180 }

// Haversine distance in kilometers
export function computeDistanceKm(a: [number, number], b: [number, number]): number {
  const lat1 = a[0], lon1 = a[1]
  const lat2 = b[0], lon2 = b[1]
  const R = 6371
  const dLat = toRad(lat2 - lat1)
  const dLon = toRad(lon2 - lon1)
  const rad1 = toRad(lat1)
  const rad2 = toRad(lat2)
  const sinDLat = Math.sin(dLat/2)
  const sinDLon = Math.sin(dLon/2)
  const aVal = sinDLat*sinDLat + sinDLon*sinDLon * Math.cos(rad1) * Math.cos(rad2)
  const c = 2 * Math.atan2(Math.sqrt(aVal), Math.sqrt(1 - aVal))
  return R * c
}

export function estimateETASecondsKm(distanceKm: number, avgKmh = 40): number {
  const hours = distanceKm / avgKmh
  return Math.round(hours * 3600)
}
