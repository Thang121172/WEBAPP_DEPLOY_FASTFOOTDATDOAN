import React, { useMemo } from 'react'
import { MapContainer, TileLayer, Marker, Popup, Polyline, useMap } from 'react-leaflet'
import 'leaflet/dist/leaflet.css'
import L from 'leaflet'
import { LatLngExpression } from 'leaflet'
import { computeDistanceKm } from '../utils/geo'

// small helper to center map when positions change
function AutoCenter({ position }: { position?: LatLngExpression }) {
  const map = useMap()
  React.useEffect(() => {
    if (position) map.setView(position as L.LatLngExpression, map.getZoom())
  }, [position, map])
  return null
}

type Provider = 'osm' | 'mapbox'

export default function MapView({
  customerPosition,
  shipperPosition,
  route,
  provider = 'osm',
}: {
  customerPosition?: [number, number]
  shipperPosition?: [number, number]
  route?: [number, number][]
  provider?: Provider
}) {
  const center = useMemo(() => customerPosition ?? shipperPosition ?? [10.762622, 106.660172], [customerPosition, shipperPosition])

  const mapboxToken = import.meta.env.VITE_MAPBOX_TOKEN || ''

  const tile = provider === 'mapbox'
    ? `https://api.mapbox.com/styles/v1/{id}/tiles/{z}/{x}/{y}?access_token=${mapboxToken}`
    : 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'

  const tileAttribution = provider === 'mapbox'
    ? 'Map data © <a href="https://www.openstreetmap.org/">OSM</a> & Mapbox'
    : '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'

  // compute ETA example (straight-line + avg speed 40 km/h)
  let etaText: string | null = null
  if (customerPosition && shipperPosition) {
    const km = computeDistanceKm(customerPosition, shipperPosition)
    const speedKmh = 40
    const hours = km / speedKmh
    const minutes = Math.round(hours * 60)
    etaText = `${minutes} min (~${km.toFixed(2)} km)`
  }

  const shipperIcon = new L.Icon({
    iconUrl: 'https://unpkg.com/leaflet@1.9.3/dist/images/marker-icon.png',
    iconSize: [25, 41],
    iconAnchor: [12, 41],
  })

  const customerIcon = new L.Icon({
    iconUrl: 'https://unpkg.com/leaflet@1.9.3/dist/images/marker-icon-red.png',
    iconSize: [25, 41],
    iconAnchor: [12, 41],
  })

  return (
    <div style={{ height: 400 }}>
      <MapContainer center={center as LatLngExpression} zoom={13} style={{ height: '100%', width: '100%' }}>
        <TileLayer
          attribution={tileAttribution}
          url={tile}
          id={provider === 'mapbox' ? 'mapbox/streets-v11' : undefined}
          tileSize={provider === 'mapbox' ? 512 : undefined}
          zoomOffset={provider === 'mapbox' ? -1 : undefined}
        />
        <AutoCenter position={center as LatLngExpression} />

        {customerPosition && (
          <Marker position={customerPosition as LatLngExpression} icon={customerIcon}>
            <Popup>Customer</Popup>
          </Marker>
        )}

        {shipperPosition && (
          <Marker position={shipperPosition as LatLngExpression} icon={shipperIcon}>
            <Popup>Shipper</Popup>
          </Marker>
        )}

        {route && route.length > 0 && (
          <Polyline positions={route as LatLngExpression[]} color="blue" />
        )}

      </MapContainer>
      {etaText && <div style={{ padding: 8, fontSize: 14 }}>ETA: {etaText}</div>}
    </div>
  )
}
