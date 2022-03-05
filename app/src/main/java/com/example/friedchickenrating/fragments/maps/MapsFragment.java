package com.example.friedchickenrating.fragments.maps;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
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
import com.example.friedchickenrating.fragments.ratings.Rating;
import com.example.friedchickenrating.fragments.ratings.RatingPlace;
import com.example.friedchickenrating.fragments.ratings.RatingViewModel;
import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryBounds;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PointOfInterest;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapsFragment extends Fragment {

    private RatingViewModel ratingViewModel;
    private FragmentMapsBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser user;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    static final int REQUEST_MAP_PLACE_FOR_ADD_RATING = 1;
    static final int REQUEST_MAP_PLACE_FOR_VIEW_RATING = 2;
    static final int REQUEST_BOTTOM_SHEET_FOR_MAP = 1;

    private GoogleMap map;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Boolean isShowCustomMarker = false;

    private String previousGeoHash = null;

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
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(mapReadyCallback);

            // Initiate the SDK for Places
            String apiKey = getString(R.string.google_maps_key);
            if (!Places.isInitialized()) {
                Places.initialize(getContext(), apiKey);
            }

            PlacesClient placesClient = Places.createClient(getContext());

            // Initialize the AutocompleteSupportFragment.
            AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                    getChildFragmentManager().findFragmentById(R.id.autocomplete_fragment);

            autocompleteFragment.setCountries("CA");
            autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
            autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(@NonNull Place place) {
                    Log.i(TAG, "Place: " + place.getName() + ", " + place.getId());
                    LatLng userLocation = new LatLng(place.getLatLng().latitude, place.getLatLng().longitude);
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 20));
                }

                @Override
                public void onError(@NonNull Status status) {
                    Log.i(TAG, "An error occurred: " + status);
                }
            });

            FloatingActionButton fab = getActivity().findViewById(R.id.fab);
            if(fab != null) {
                fab.setVisibility(View.INVISIBLE);
            }
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

                    String currentGeoHash = GeoFireUtils.getGeoHashForLocation(
                            new GeoLocation(location.getLatitude(), location.getLongitude()));

                    if (!currentGeoHash.equals(previousGeoHash)) {
                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                        Log.d(TAG, "onLocationChanged, latitude: " + latLng.latitude + ", longitude: " + latLng.longitude);
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17));
                        previousGeoHash = currentGeoHash;
                    }
                }
            };

            enableMyLocation();
        }
    };

    private boolean checkPermission() {
        int hasFineLocationPermission =
                ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION);

        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED) {
            return true;
        }

        return false;
    }

    private void requestPermission() {
        permissionResultCallback.launch(android.Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private void enableMyLocation() {

        if (checkPermission()) {

            if (map != null) {

                map.setMyLocationEnabled(true);
                map.getUiSettings().setMyLocationButtonEnabled(true);
                map.getUiSettings().setZoomControlsEnabled(true);
                map.getUiSettings().setZoomGesturesEnabled(true);

                map.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
                    @Override
                    public boolean onMyLocationButtonClick() {

                        Log.d(TAG, "Click My Location Button");
                        return false;
                    }
                });

                //Get current location
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, 1000, 0, locationListener);

                Location lastUserKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if(lastUserKnownLocation != null) {
                    LatLng userLocation = new LatLng(lastUserKnownLocation.getLatitude(),
                            lastUserKnownLocation.getLongitude());

                    previousGeoHash = GeoFireUtils.getGeoHashForLocation(
                            new GeoLocation(userLocation.latitude, userLocation.longitude));

                    //Request to point a place from View Rating
                    Integer requestCode = ratingViewModel.getMapRequestCode().getValue();
                    if (requestCode != null && requestCode == REQUEST_MAP_PLACE_FOR_VIEW_RATING) {

                        RatingPlace place = ratingViewModel.getSelectedRatingPlace().getValue();

                        Log.d(TAG, "selcted place: " + place.getPlaceid());
                        Log.d(TAG, "latitude: " + place.getLatitude() + ", longitude: " + place.getLongitude());
                        LatLng placeLocation = new LatLng(place.getLatitude(), place.getLongitude());

                        Log.d(TAG, "map: " + map);
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(placeLocation, 20));

                        MarkerOptions markerOptions = new MarkerOptions().position(placeLocation);
                        markerOptions.title(place.getName());

                        Marker marker = map.addMarker(markerOptions);
                        marker.setSnippet(place.getPlaceid());
                        marker.setZIndex(10);
                    } else {

                        //map.addMarker(new MarkerOptions().position(userLocation).title("your Location"));
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 17));
                    }

                    //register event handler to click icon of place
                    map.setOnPoiClickListener(new GoogleMap.OnPoiClickListener() {
                        @Override
                        public void onPoiClick(@NonNull PointOfInterest pointOfInterest) {
                            Log.d(TAG, "onPoiClick, latitude: " + pointOfInterest.latLng.latitude
                                    + ", longitude: " + pointOfInterest.latLng.longitude
                                    + ", pointOfInterest.name: " + pointOfInterest.name
                                    + ", pointOfInterest.placeId: " + pointOfInterest.placeId);

                            String region = ratingViewModel.getRegionFromLatLng(requireContext(),
                                    pointOfInterest.latLng.latitude, pointOfInterest.latLng.longitude);

                            Integer requestCode = ratingViewModel.getMapRequestCode().getValue();
                            if (requestCode != null && requestCode == REQUEST_MAP_PLACE_FOR_ADD_RATING) {
                                Bundle result = new Bundle();
                                result.putString("placeId", pointOfInterest.placeId);
                                result.putString("placeName", pointOfInterest.name);
                                result.putDouble("latitude", pointOfInterest.latLng.latitude);
                                result.putDouble("longitude", pointOfInterest.latLng.longitude);
                                result.putString("region", region);

                                getParentFragmentManager().setFragmentResult("passByMapPlace", result);

                                NavHostFragment.findNavController(MapsFragment.this)
                                        .navigate(R.id.action_nav_maps_to_nav_newRating);
                            } else {
                                RatingPlace ratingPlace = new RatingPlace(
                                        pointOfInterest.placeId,
                                        pointOfInterest.name,
                                        pointOfInterest.latLng.latitude,
                                        pointOfInterest.latLng.longitude,
                                        "",
                                        region);

                                ratingViewModel.setSelectedRatingPlace(ratingPlace);
                                ratingViewModel.setMapRequestCode(REQUEST_BOTTOM_SHEET_FOR_MAP);
                                BottomSheetFragment bottomSheetFragment
                                        = BottomSheetFragment.newInstance(
                                        pointOfInterest.placeId,
                                        pointOfInterest.name,
                                        region);
                                bottomSheetFragment.show(getParentFragmentManager(), BottomSheetFragment.TAG);
                            }
                        }
                    });

                    //register event handler to click any point of map
                    map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                        @Override
                        public void onMapClick(@NonNull LatLng latLng) {
                            //map.addMarker(new MarkerOptions().position(latLng).title("Selected Place"));
                            Log.d(TAG, "onMapClick, latitude: " + latLng.latitude + ", longitude: " + latLng.longitude);
                        }
                    });

                    //register event handler to click a custom marker of map
                    //However, custom marker puts on a layer on the map.
                    //So, when user clicks the marker, event happens by clicking a POI
                    map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                        @Override
                        public boolean onMarkerClick(@NonNull Marker marker) {
                            //String markerName = marker.getTitle();
                            //Toast.makeText(getContext(), "Clicked location is " + markerName + ", marker id: " + marker.getId(), Toast.LENGTH_SHORT).show();

                            String placeId = marker.getSnippet();
                            if (marker.getSnippet() != null) {
                                db.collection("places").whereEqualTo("placeid", placeId)
                                        .addSnapshotListener(new EventListener<QuerySnapshot>() {
                                            @Override
                                            public void onEvent(@Nullable QuerySnapshot value,
                                                                @Nullable FirebaseFirestoreException error) {
                                                if (error != null) {
                                                    Log.w(TAG, "Listen failed.", error);
                                                    return;
                                                }

                                                if (!value.isEmpty()) {
                                                    QueryDocumentSnapshot document = (QueryDocumentSnapshot) value.getDocuments().get(0);
                                                    if (document != null) {
                                                        Log.d(TAG, document.getId() + " => " + document.getData());
                                                        RatingPlace place = document.toObject(RatingPlace.class);

                                                        ratingViewModel.setSelectedRatingPlace(place);
                                                        ratingViewModel.setMapRequestCode(REQUEST_BOTTOM_SHEET_FOR_MAP);
                                                        BottomSheetFragment bottomSheetFragment
                                                                = BottomSheetFragment.newInstance(
                                                                place.getPlaceid(),
                                                                place.getName(),
                                                                place.getRegion());
                                                        bottomSheetFragment.show(getParentFragmentManager(), BottomSheetFragment.TAG);
                                                    }
                                                }
                                            }
                                        });
                            }

                            return true;
                        }
                    });

                    //Register event handler of Map Layer Image Button
                    binding.btnMapLayer.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (isShowCustomMarker == false) {
                                binding.btnMapLayerGroup.setVisibility(View.VISIBLE);
                                binding.btnMapLayerPerson.setVisibility(View.VISIBLE);

                                placeMarkerOnMap(userLocation, 1); // Default: rating places of all user
                                isShowCustomMarker = true;
                            } else {
                                binding.btnMapLayerGroup.setVisibility(View.GONE);
                                binding.btnMapLayerPerson.setVisibility(View.GONE);

                                map.clear();
                                isShowCustomMarker = false;
                            }
                        }
                    });

                    binding.btnMapLayerGroup.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            map.clear();
                            placeMarkerOnMap(userLocation, 1); // rating places of all user
                        }
                    });

                    binding.btnMapLayerPerson.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            map.clear();
                            placeMarkerOnMap(userLocation, 2); // rating places of only login user
                        }
                    });
                }
            }

        } else {
            requestPermission();
        }
    }

    private void placeMarkerOnMap(LatLng currentLocation, int filter) {

        //find places to be matched with user's current location
        final GeoLocation center = new GeoLocation(currentLocation.latitude, currentLocation.longitude);
        final double radiusInMeter = 50 * 1000; //find places within 50km

        List<GeoQueryBounds> bounds = GeoFireUtils.getGeoHashQueryBounds(center, radiusInMeter);
        final List<Task<QuerySnapshot>> tasks = new ArrayList<>();
        for (GeoQueryBounds b : bounds) {
            Query q = db.collection("places")
                    .orderBy("geohash")
                    .startAt(b.startHash)
                    .endAt(b.endHash);

            tasks.add(q.get());
        }

        List<RatingPlace> placeList = new ArrayList<>();
        Tasks.whenAllComplete(tasks)
                .addOnCompleteListener(new OnCompleteListener<List<Task<?>>>() {
                    @Override
                    public void onComplete(@NonNull Task<List<Task<?>>> t) {
                        List<DocumentSnapshot> matchingPlaceDocs = new ArrayList<>();

                        for (Task<QuerySnapshot> task : tasks) {
                            QuerySnapshot snap = task.getResult();
                            for (DocumentSnapshot doc : snap.getDocuments()) {
                                double latitude = doc.getDouble("latitude");
                                double longitude = doc.getDouble("longitude");

                                // We have to filter out a few false positives due to GeoHash
                                // accuracy, but most will match
                                GeoLocation docLocation = new GeoLocation(latitude, longitude);
                                double distanceInMeter = GeoFireUtils.getDistanceBetween(docLocation, center);
                                if (distanceInMeter <= radiusInMeter) {
                                    matchingPlaceDocs.add(doc);
                                    RatingPlace place = doc.toObject(
                                            RatingPlace.class);
                                    placeList.add(place);
                                }
                            }
                        }

                        // matching Docs with places
                        for (int i = 0; i < placeList.size(); i++) {
                            LatLng location = new LatLng(placeList.get(i).getLatitude(), placeList.get(i).getLongitude());
                            String placeName = placeList.get(i).getName();
                            String placeId = placeList.get(i).getPlaceid();

                            //query rating list with place id
                            Query query;
                            if(filter == 1) { // rating places of all user
                                query = db.collection("ratings").whereEqualTo("placeid", placeId);
                            } else { // rating places of only login user
                                query = db.collection("ratings")
                                                .whereEqualTo("placeid", placeId)
                                                .whereEqualTo("userid", user.getUid());
                            }
                            query.addSnapshotListener(new EventListener<QuerySnapshot>() {
                                @Override
                                public void onEvent(@Nullable QuerySnapshot value,
                                                    @Nullable FirebaseFirestoreException error) {
                                    if(error != null) {
                                        Log.w(TAG, "Listen failed.", error);
                                        return;
                                    }

                                    if(value.size() > 0) {
                                        Double sum = 0.0;
                                        for (QueryDocumentSnapshot document : value) {
                                            if (document != null) {
                                                Log.d(TAG, document.getId() + " => " + document.getData());
                                                Rating rating = document.toObject(Rating.class);

                                                sum += rating.getStaroverall();
                                                Log.d(TAG, "sum: " + sum + ", each star overall: " + rating.getStaroverall());
                                            }
                                        }

                                        int starDrawable;
                                        Double overallStar = sum / value.size();
                                        Log.d(TAG, "overallStar: " + overallStar);
                                        if (overallStar > 4.0 && overallStar <= 5.0) {
                                            starDrawable = R.drawable.emoticons5;
                                        } else if (overallStar > 3.0 && overallStar <= 4.0) {
                                            starDrawable = R.drawable.emoticons4;
                                        } else if (overallStar > 2.0 && overallStar <= 3.0) {
                                            starDrawable = R.drawable.emoticons3;
                                        } else if (overallStar > 1.0 && overallStar <= 2.0) {
                                            starDrawable = R.drawable.emoticons2;
                                        } else {
                                            starDrawable = R.drawable.emoticons1;
                                        }

                                        MarkerOptions markerOptions = new MarkerOptions().position(location);
                                        markerOptions.title(placeName);

                                        //markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                                        Drawable drawableIcon = ResourcesCompat.getDrawable(getResources(), starDrawable, null);
                                        BitmapDescriptor icon = getMarkerIconFromDrawable(drawableIcon);
                                        markerOptions.icon(icon);

                                        Marker marker = map.addMarker(markerOptions);
                                        marker.setSnippet(placeId);
                                        marker.setZIndex(10);
                                        //marker.showInfoWindow();
                                    }
                                }
                            });
                        }
                    }
                });
    }

    private BitmapDescriptor getMarkerIconFromDrawable(Drawable drawable) {
        Canvas canvas = new Canvas();
        //Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        //drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.setBounds(0,0, 100, 100);
        drawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private ActivityResultLauncher<String> permissionResultCallback = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean result) {
                    if (result) { // permission granted
                        enableMyLocation();
                    }
                }
            });


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}