package com.dicoding.skripsiapp

import androidx.fragment.app.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.dicoding.skripsiapp.databinding.FragmentMapsBinding
import com.google.android.gms.maps.model.*

class MapsFragment : Fragment(R.layout.fragment_maps) {

    private var _binding: FragmentMapsBinding? = null
    private val binding get() = _binding!!

    private lateinit var googleMap: GoogleMap


//    private val callback = OnMapReadyCallback { googleMap ->
//        /**
//         * Manipulates the map once available.
//         * This callback is triggered when the map is ready to be used.
//         * This is where we can add markers or lines, add listeners or move the camera.
//         * In this case, we just add a marker near Sydney, Australia.
//         * If Google Play services is not installed on the device, the user will be prompted to
//         * install it inside the SupportMapFragment. This method will only be triggered once the
//         * user has installed Google Play services and returned to the app.
//         */
//        val sydney = LatLng(-34.0, 151.0)
//        googleMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
//        googleMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
//    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_maps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync { map ->
            googleMap = map
            loadLarvaMarkers()
        }
    }

    private fun loadLarvaMarkers() {
        val detections = getSavedDetections()

        detections.forEach { detection ->

            val color = when (detection.classId) {
                0 -> BitmapDescriptorFactory.HUE_RED
                1 -> BitmapDescriptorFactory.HUE_GREEN
                2 -> BitmapDescriptorFactory.HUE_BLUE
                else -> BitmapDescriptorFactory.HUE_YELLOW
            }

            val marker = MarkerOptions()
                .position(LatLng(detection.latitude, detection.longitude))
                .title(detection.label)
                .snippet("Confidence: ${detection.confidence}%")
                .icon(BitmapDescriptorFactory.defaultMarker(color))

            googleMap.addMarker(marker)
        }

        if (detections.isNotEmpty()) {
            val first = detections.first()
            googleMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(first.latitude, first.longitude),
                    12f
                )
            )
        }
    }



    //dummy data
    private fun getSavedDetections(): List<LarvaDetection> {
        return listOf(
            LarvaDetection(3.5952, 98.6722, 0, "Aedes", 95f, System.currentTimeMillis()),
            LarvaDetection(3.6010, 98.6700, 1, "Anopheles", 91f, System.currentTimeMillis()),
            LarvaDetection(3.5900, 98.6800, 2, "Culex", 88f, System.currentTimeMillis())
        )

    }

}