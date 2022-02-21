package com.example.friedchickenrating.fragments.maps;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.friedchickenrating.R;
import com.example.friedchickenrating.databinding.FragmentMapsBinding;
import com.example.friedchickenrating.fragments.ratings.NewRatingFragment;
import com.example.friedchickenrating.fragments.ratings.Rating;
import com.example.friedchickenrating.fragments.ratings.RatingViewModel;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PointOfInterest;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Arrays;

public class MapsFragment extends Fragment {

    private RatingViewModel ratingViewModel;
    private FragmentMapsBinding binding;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    static final int REQUEST_MAP_PLACE_FOR_ADD_RATING = 1;

    private GoogleMap map;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Marker currentMarker = null;

    private static final String TAG = "MapsFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentMapsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ratingViewModel = new ViewModelProvider(requireActivity()).get(RatingViewModel.class);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(mapReadyCallback);

            // Initiate the SDK for Places
            String apiKey = getString(R.string.api_key);
            if(!Places.isInitialized()) {
                Places.initialize(getContext(), apiKey);
            }

            PlacesClient placesClient = Places.createClient(getContext());

            // Initialize the AutocompleteSupportFragment.
            AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                    getChildFragmentManager().findFragmentById(R.id.autocomplete_fragment);

//            // Set location bound for autocomplete to Vancouver city
//            autocompleteFragment.setLocationBias(RectangularBounds.newInstance(
//                    new LatLng(49.261111, -123.113889),
//                    new LatLng(49.261111, -123.113889)
//            ));

            autocompleteFragment.setCountries("CA");
            autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
            autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(@NonNull Place place) {
                    Log.i(TAG, "Place: " + place.getName() + ", " + place.getId());
                    LatLng userLocation = new LatLng(place.getLatLng().latitude,
                            place.getLatLng().longitude);
                    map.addMarker(new MarkerOptions().position(userLocation).title(place.getName()));
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 16));

                    Integer requestCode = ratingViewModel.getMapRequestCode().getValue();
                    if(requestCode != null && requestCode == REQUEST_MAP_PLACE_FOR_ADD_RATING) {
                        Bundle result = new Bundle();
                        result.putString("placeId", place.getId());
                        result.putString("placeName", place.getName());
                        result.putDouble("latitude", place.getLatLng().latitude);
                        result.putDouble("longitude", place.getLatLng().longitude);

                        getParentFragmentManager().setFragmentResult("requestMapPlaceInfo", result);

                        NavHostFragment.findNavController(MapsFragment.this)
                                .navigate(R.id.action_nav_maps_to_nav_newRating);
                    }
                }

                @Override
                public void onError(@NonNull Status status) {
                    Log.i(TAG, "An error occurred: " + status);
                }
            });

            FloatingActionButton fab = getActivity().findViewById(R.id.fab);
            fab.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onStop() {
        FloatingActionButton fab = getActivity().findViewById(R.id.fab);
        fab.setVisibility(View.VISIBLE);

        super.onStop();
    }

    private OnMapReadyCallback mapReadyCallback = new OnMapReadyCallback() {
        @Override
        public void onMapReady(GoogleMap googleMap) {
            map = googleMap;

            //Initiate LocationManager and LocationListener for current location of user
            locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
//                    Log.d(TAG, "onLocationChanged, latitude: " + latLng.latitude + ", longitude: " + latLng.longitude);
                }
            };

            enableMyLocation();
        }
    };

    private boolean checkPermission() {
        int hasFineLocationPermission =
                ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission =
                ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION);

        if(hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasFineLocationPermission == PackageManager.PERMISSION_GRANTED) {
            return true;
        }

        return false;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(getActivity(),
                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    private void enableMyLocation() {

        if(checkPermission()) {

            if(map != null) {
                //Get current location
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, 0, 0, locationListener);

                Location lastUserKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                LatLng userLocation = new LatLng(lastUserKnownLocation.getLatitude(),
                        lastUserKnownLocation.getLongitude());
                map.addMarker(new MarkerOptions().position(userLocation).title("your Location"));
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 16));

                map.setMyLocationEnabled(true);
                map.getUiSettings().setMyLocationButtonEnabled(true);
                map.getUiSettings().setZoomControlsEnabled(true);
                map.getUiSettings().setZoomGesturesEnabled(true);

                map.setOnPoiClickListener(new GoogleMap.OnPoiClickListener() {
                    @Override
                    public void onPoiClick(@NonNull PointOfInterest pointOfInterest) {
                        Log.d(TAG, "onPoiClick, latitude: " + pointOfInterest.latLng.latitude
                                + ", longitude: " + pointOfInterest.latLng.longitude
                                + ", pointOfInterest.name: " + pointOfInterest.name
                                + ", pointOfInterest.placeId: " + pointOfInterest.placeId);

                        Integer requestCode = ratingViewModel.getMapRequestCode().getValue();
                        if(requestCode != null && requestCode == REQUEST_MAP_PLACE_FOR_ADD_RATING) {
                            Bundle result = new Bundle();
                            result.putString("placeId", pointOfInterest.placeId);
                            result.putString("placeName", pointOfInterest.name);
                            result.putDouble("latitude", pointOfInterest.latLng.latitude);
                            result.putDouble("longitude", pointOfInterest.latLng.longitude);

                            getParentFragmentManager().setFragmentResult("requestMapPlaceInfo", result);

                            NavHostFragment.findNavController(MapsFragment.this)
                                    .navigate(R.id.action_nav_maps_to_nav_newRating);
                        }
                    }
                });

                map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(@NonNull LatLng latLng) {
                        map.addMarker(new MarkerOptions().position(latLng).title("Selected Place"));
                        Log.d(TAG, "onMapClick, latitude: " + latLng.latitude + ", longitude: " + latLng.longitude);
                    }
                });

                map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(@NonNull Marker marker) {

                        String markerName = marker.getTitle();
                        Toast.makeText(getContext(), "Clicked location is " + markerName, Toast.LENGTH_SHORT).show();

                        if(marker.isInfoWindowShown()){
                            marker.hideInfoWindow();
                        }else {
                            marker.showInfoWindow();
                        }
                        return true;
                    }
                });

                //Register event handler of Map Layer Image Button
                binding.btnMapLayer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(currentMarker == null) {
                            placeMarkerOnMap();
                        } else {
                            currentMarker.remove();
                            currentMarker = null;
                        }
                    }
                });
            }
        } else {
            requestPermission();
        }
    }

    private void placeMarkerOnMap() {
//        currentMarker = map.addMarker(new MarkerOptions().position(userLocation)
//                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

//        LatLng location = new LatLng();
//        MarkerOptions markerOptions = new MarkerOptions().position(location);
//        markerOptions.title(placeName);
//        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
//        currentMarker = map.addMarker(markerOptions);
    }

    private ActivityResultLauncher<String> permissionResultCallback = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean result) {
                    if(result) { // permission granted
                        enableMyLocation();
                    }
                }
            });
}