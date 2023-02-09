package com.example.pickmeuptom

import android.Manifest.permission
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.tomtom.sdk.location.GeoPoint
import com.tomtom.sdk.location.android.AndroidLocationProvider
import com.tomtom.sdk.map.display.MapOptions
import com.tomtom.sdk.map.display.TomTomMap
import com.tomtom.sdk.map.display.location.LocationMarkerOptions
import com.tomtom.sdk.map.display.route.Instruction
import com.tomtom.sdk.map.display.route.RouteOptions
import com.tomtom.sdk.map.display.ui.MapFragment
import com.tomtom.sdk.routing.RoutePlanner
import com.tomtom.sdk.routing.RoutePlanningCallback
import com.tomtom.sdk.routing.RoutePlanningResponse
import com.tomtom.sdk.routing.RoutingFailure
import com.tomtom.sdk.routing.online.OnlineRoutePlanner
import com.tomtom.sdk.routing.options.Itinerary
import com.tomtom.sdk.routing.options.RoutePlanningOptions
import com.tomtom.sdk.routing.options.calculation.AlternativeRoutesOptions
import com.tomtom.sdk.routing.options.calculation.CostModel
import com.tomtom.sdk.routing.options.calculation.RouteType
import com.tomtom.sdk.routing.route.Route
import com.tomtom.sdk.vehicle.Vehicle.Truck


class MainActivity : AppCompatActivity() {

    private lateinit var tomTomMap: TomTomMap
    private lateinit var locationEngine: AndroidLocationProvider
    private lateinit var planRouteOptions: RoutePlanningOptions
    private lateinit var routingApi: RoutePlanner


    private lateinit var route: Route

    private val API_KEY: String = "41p1u6CyqXmvR0pPTGrH9aBISUbAI4CS"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mapOptions = MapOptions(mapKey = API_KEY)
        val mapFragment = MapFragment.newInstance(mapOptions)
        supportFragmentManager.beginTransaction()
            .replace(R.id.map_container, mapFragment)
            .commit()

        routingApi = OnlineRoutePlanner.create(this, API_KEY)

        mapFragment.getMapAsync { map ->
            tomTomMap = map
            enableUserLocation()
            setUpMapListeners()
        }

    }

    private fun enableUserLocation() {
        locationEngine = AndroidLocationProvider(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(permission.ACCESS_FINE_LOCATION), 0)
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        locationEngine.enable()

        tomTomMap.setLocationProvider(locationEngine)
        val locationMarker = LocationMarkerOptions(type=LocationMarkerOptions.Type.Pointer)
        tomTomMap.enableLocationMarker(locationMarker)
    }

    private val planRouteCallback = object: RoutePlanningCallback {
        override fun onFailure(failure: RoutingFailure) {
            Toast.makeText(this@MainActivity, failure.message, Toast.LENGTH_SHORT).show()
        }

        override fun onRoutePlanned(route: Route) {
            Log.wtf("implan", "nin bro")
        }

        override fun onSuccess(result: RoutePlanningResponse) {

            route = result.routes.first()
            drawRoute(route)
        }

    }

    private fun Route.mapInstructions(): List<Instruction> {
        val routeInstructions = legs.flatMap { routeLeg -> routeLeg.instructions }
        return routeInstructions.map {
            Instruction(
                routeOffset = it.routeOffset,
                combineWithNext = it.combineWithNext
            )
        }
    }

    private fun drawRoute(route: Route) {
        val instructions = route.mapInstructions()
        val geometry = route.legs.flatMap { it.points }
        val routeOptions = RouteOptions(
            geometry = geometry,
            destinationMarkerVisible = true,
            departureMarkerVisible = true,
            instructions = instructions
        )
        tomTomMap.addRoute(routeOptions)

        val ZOOM_PADDING = 20

        tomTomMap.zoomToRoutes(ZOOM_PADDING)


    }

    private fun createRoute(destination: GeoPoint) {
        val userLocation = tomTomMap.currentLocation?.position ?: return
        val itinerary = Itinerary(origin = userLocation, destination = destination)

        planRouteOptions = RoutePlanningOptions(
            itinerary = itinerary,
            costModel = CostModel(routeType = RouteType.Efficient),
            vehicle = Truck(),
            alternativeRoutesOptions = AlternativeRoutesOptions(maxAlternatives = 2)
            )
        Log.wtf("planroutecallback", planRouteCallback.toString())
        routingApi.planRoute(planRouteOptions, planRouteCallback)
    }

    private fun setUpMapListeners() {
        tomTomMap.addMapLongClickListener{ coordinate: GeoPoint ->
            createRoute(coordinate)
            return@addMapLongClickListener true
        }
    }
}