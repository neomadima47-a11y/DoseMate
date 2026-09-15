package com.example.data

class MapboxClient {
    fun searchPlaces(query: String, proximityLat: Double, proximityLng: Double): List<MapboxPlaceResult> {
        // Mock search results
        return listOf(
            MapboxPlaceResult("Hatfield Clinic", "Pretoria, South Africa", -25.7479, 28.2293),
            MapboxPlaceResult("Sunnyside Medical", "Sunnyside, Pretoria", -25.7511, 28.2045)
        )
    }

    fun getDirections(
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double,
        profile: String
    ): MapboxRoute {
        return MapboxRoute(
            coordinates = listOf(
                originLng to originLat,
                destLng to destLat
            ),
            formattedDuration = "12 min",
            formattedDistance = "4.5 km"
        )
    }

    fun buildMapHtml(
        token: String,
        userLat: Double,
        userLng: Double,
        clinicsJson: String,
        selectedClinicId: String?,
        routeGeoJson: String,
        destLat: Double?,
        destLng: Double?,
        destName: String?,
        styleId: String
    ): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>Mapbox Map</title>
                <meta name="viewport" content="initial-scale=1,maximum-scale=1,user-scalable=no">
                <script src="https://api.mapbox.com/mapbox-gl-js/v3.9.4/mapbox-gl.js"></script>
                <link href="https://api.mapbox.com/mapbox-gl-js/v3.9.4/mapbox-gl.css" rel="stylesheet">
                <style>
                    body { margin: 0; padding: 0; background-color: #e5e5e5; }
                    #map { position: absolute; top: 0; bottom: 0; width: 100%; height: 100%; }
                    .loading-overlay {
                        position: absolute; top: 0; left: 0; right: 0; bottom: 0;
                        display: flex; align-items: center; justify-content: center;
                        background: #f8f9fa; z-index: 999; font-family: sans-serif;
                    }
                </style>
            </head>
            <body>
                <div id="loading" class="loading-overlay">Initializing Map...</div>
                <div id="map"></div>
                <script>
                    console.log('Mapbox token length: ' + '$token'.length);
                    
                    try {
                        console.log('Mapbox script status: ' + (typeof mapboxgl !== 'undefined' ? 'Loaded' : 'Missing'));
                        if (typeof mapboxgl === 'undefined' || !mapboxgl.supported()) {
                            document.getElementById('loading').innerHTML = '<div style="color:red">WebGL / Mapbox GL Not Supported</div>';
                        } else {
                            mapboxgl.accessToken = '$token'.trim();
                            console.log('Token check: ' + (mapboxgl.accessToken.startsWith('pk.') ? 'Valid prefix' : 'Invalid prefix'));
                            
                            const map = new mapboxgl.Map({
                                container: 'map',
                                style: 'mapbox://styles/mapbox/streets-v11',
                                center: [$userLng, $userLat],
                                zoom: 11,
                                attributionControl: true
                            });
                            
                            map.addControl(new mapboxgl.NavigationControl());

                            map.on('load', () => {
                                document.getElementById('loading').style.display = 'none';
                                console.log('Map successfully loaded');
                                
                                // User location
                                new mapboxgl.Marker({ color: '#007bff' })
                                    .setLngLat([$userLng, $userLat])
                                    .addTo(map);

                                // Clinics
                                const clinics = $clinicsJson;
                                if (Array.isArray(clinics)) {
                                    clinics.forEach(c => {
                                        const marker = new mapboxgl.Marker({ color: c.id === '$selectedClinicId' ? '#dc3545' : '#28a745' })
                                            .setLngLat([parseFloat(c.lng), parseFloat(c.lat)])
                                            .setPopup(new mapboxgl.Popup().setHTML('<b>' + c.name + '</b>'))
                                            .addTo(map);
                                        
                                        marker.getElement().addEventListener('click', () => {
                                            if (window.AndroidBridge) AndroidBridge.onClinicSelected(c.id);
                                        });
                                    });
                                }

                                // Route
                                const route = $routeGeoJson;
                                if (route && route !== null && route !== 'null') {
                                    map.addSource('route', {
                                        'type': 'geojson',
                                        'data': {
                                            'type': 'Feature',
                                            'geometry': { 'type': 'LineString', 'coordinates': route }
                                        }
                                    });
                                    map.addLayer({
                                        'id': 'route', 'type': 'line', 'source': 'route',
                                        'layout': { 'line-join': 'round', 'line-cap': 'round' },
                                        'paint': { 'line-color': '#007bff', 'line-width': 6, 'line-opacity': 0.8 }
                                    });
                                }
                            });

                            map.on('error', (e) => {
                                console.error('Mapbox GL Error:', e.error?.message || e);
                                // Don't hide loading if it's a fatal auth error
                                if (e.error?.status === 401) {
                                    document.getElementById('loading').innerHTML = '<div style="color:red">Mapbox Auth Failed (401)</div>';
                                }
                            });
                            
                            window.recenterMap = () => { map.flyTo({ center: [$userLng, $userLat], zoom: 14 }); };
                        }
                    } catch (err) {
                        console.error('JS Crash:', err);
                        document.getElementById('loading').innerText = 'JS Error: ' + err.message;
                    }
                </script>
            </body>
            </html>
        """.trimIndent()
    }
}
