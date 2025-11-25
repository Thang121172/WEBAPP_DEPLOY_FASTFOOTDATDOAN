Maps integration
=================

This document describes how to integrate maps (Leaflet + tile providers), route/ETA estimation, privacy and guest flows for the FASTFOOD frontend.

1) Libraries and providers
- Leaflet (works offline-friendly and with many tile providers)
- Mapbox (proprietary tiles, needs token)
- Google Maps JavaScript API + Directions API (route and ETA with traffic)

Frontend components
- `src/components/MapView.tsx` - Leaflet based map that can use OSM or Mapbox tiles. It shows customer and shipper markers and optional polyline route.

2) Directions & ETA
- Google Directions API: best accuracy and traffic-aware ETA. Requires billing and API key.
- Simple ETA fallback: use Haversine distance (straight line) and an average speed (e.g., 30-45 km/h) to estimate ETA. This is implemented in `src/utils/geo.ts`.
- If you integrate Google Directions, call their Directions API server-side (to hide API key) or use a server proxy. Parse the `legs.duration.value` for ETA in seconds and `overview_polyline` for route points.

3) Realtime shipper updates
- Use WebSocket (Channels/Socket.IO) or WebTransport to broadcast shipper positions to only authorized clients.
- Throttle shipper updates on client side: 3s-10s interval depending on desired accuracy. See `src/utils/shipper.ts`.
- For best efficiency, shipper should send lat/lng only when moved a threshold distance (e.g., >5-10 meters) or after N seconds.

4) Privacy & Security
- Always ask for explicit user consent before capturing or sending location coordinates. Use browser `permissions` API to check state and show UI to request consent. See `src/hooks/useLocationConsent.tsx`.
- Only broadcast positions to users who are allowed to see them (order owner). Implement backend authorization in the WebSocket channel to validate the connecting user and the order ID.
- Provide a setting for users to delete/obfuscate addresses (GDPR-like). When requested, either remove address from DB or mark it as hidden and stop broadcasting.
- Store precise coordinates only as long as needed. Consider storing only coarse location for analytics.

5) Guest flow
- Store guest address in `localStorage` or cookie. Prompt guest to register after N orders, or when they click "Save this address".
- If geolocation permission is denied, require form fields: Street, Ward, District, City. Add a "Get current position" button that re-requests permission and can autofill lat/lng when allowed.

6) Implementation notes
- Avoid embedding secret API keys in frontend. Use server side proxy for Google Directions or Mapbox secret operations.
- Tile usage: OSM is free with a rate limit; Mapbox requires a token and has terms.

Files added:
- `src/components/MapView.tsx`
- `src/utils/geo.ts`
- `src/hooks/useLocationConsent.tsx`
- `src/utils/shipper.ts`


*** End of file"}