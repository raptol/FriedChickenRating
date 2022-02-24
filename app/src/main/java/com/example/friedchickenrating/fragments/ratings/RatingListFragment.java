package com.example.friedchickenrating.fragments.ratings;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.example.friedchickenrating.R;
import com.example.friedchickenrating.databinding.FragmentRatingListBinding;
import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryBounds;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class RatingListFragment extends Fragment implements RatingListAdapter.ItemClickListener{

    private static RatingListFragment instance = null;
    private RatingViewModel ratingViewModel;
    private FragmentRatingListBinding binding;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser user;

    private List<Rating> ratingList;
    private List<RatingPlace> placeList;
    private RatingListAdapter ratingListAdapter;

    private static final String TAG = RatingListFragment.class.getSimpleName();

    private static final int SORT_OPTION_LOCATION = 1;
    private static final int SORT_OPTION_HIGH_STARS = 2;
    private static final int SORT_OPTION_LATEST = 3;
    private static final int SORT_OPTION_RELEVANT = 4;
    private static final int SORT_OPTION_MY_RATING = 5;

    private LocationManager locationManager;
    private LocationListener locationListener;
    private LatLng userLocation;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
    }

    public static RatingListFragment getInstance() {
        return instance;
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ratingViewModel = new ViewModelProvider(requireActivity()).get(RatingViewModel.class);

        binding = FragmentRatingListBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        ratingList = new ArrayList<>();
        placeList = new ArrayList<>();

        ratingListAdapter = new RatingListAdapter(ratingList, placeList);
        ratingListAdapter.setListener(this);

        binding.recyclerViewRatingList.setLayoutManager(new GridLayoutManager(this.getContext(), 1));
        binding.recyclerViewRatingList.setAdapter(ratingListAdapter);

        //Check permission to get current location
        if (!checkPermission()) {
            requestPermission();
        }

        //Get current location
        //Initiate LocationManager and LocationListener for current location of user
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
//                    Log.d(TAG, "onLocationChanged, latitude: " + latLng.latitude + ", longitude: " + latLng.longitude);
            }
        };

        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 0, 0, locationListener);

        Location lastUserKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        userLocation = new LatLng(lastUserKnownLocation.getLatitude(), lastUserKnownLocation.getLongitude());

        //display rating list
        displayRatingList(SORT_OPTION_LOCATION); //default sorting option: location

        binding.btnSortLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayRatingList(SORT_OPTION_LOCATION);
            }
        });

        binding.btnSortHighRates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayRatingList(SORT_OPTION_HIGH_STARS);
            }
        });

        binding.btnSortLatest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayRatingList(SORT_OPTION_LATEST);
            }
        });

        binding.btnSortRelevant.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayRatingList(SORT_OPTION_RELEVANT);
            }
        });

        binding.btnSortMyRates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayRatingList(SORT_OPTION_MY_RATING);
            }
        });

        //event handler for open map button
        binding.btnOpenMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(RatingListFragment.this)
                        .navigate(R.id.action_nav_ratings_to_nav_maps);
            }
        });
    }

    private boolean checkPermission() {
        int hasFineLocationPermission =
                ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission =
                ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION);

        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
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


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onListItemClick(Rating rating, int position) {
        ratingViewModel.setSelectedRating(rating);
        ratingViewModel.setSelectedRatingId(ratingList.get(position).getId());

        NavHostFragment.findNavController(RatingListFragment.this)
                .navigate(R.id.action_nav_ratings_to_nav_viewRatings);
    }

    private void displayRatingList(int sortOption) {

        Query query;

        switch(sortOption) {
            case SORT_OPTION_HIGH_STARS:
                query = db.collection("ratings")
                        .orderBy("staroverall", Query.Direction.DESCENDING);
                readPlaceList();
                readRatingList(query);
                break;

            case SORT_OPTION_LATEST:
                query = db.collection("ratings")
                        .orderBy("timestamp", Query.Direction.DESCENDING);
                readPlaceList();
                readRatingList(query);
                break;

            case SORT_OPTION_RELEVANT:
                query = db.collection("ratings")
                        .orderBy("title")
                        .orderBy("placeid");
                readPlaceList();
                readRatingList(query);
                break;

            case SORT_OPTION_MY_RATING:
                query = db.collection("ratings")
                        .whereEqualTo("userid", user.getUid())
                        .orderBy("timestamp", Query.Direction.DESCENDING);
                readPlaceList();
                readRatingList(query);
                break;

            default: // by location
                readRatingListByCurrentLocation(userLocation);
        }

        //Scroll to top
        binding.recyclerViewRatingList.smoothScrollToPosition(0);
    }

    private void readPlaceList() {
        // Listen for realtime updates of the places
        db.collection("places")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value,
                                        @Nullable FirebaseFirestoreException error) {
                        if(error != null) {
                            Log.w(TAG, "Listen failed.", error);
                            return;
                        }

                        placeList.clear();
                        for(QueryDocumentSnapshot document: value) {
                            if (document != null) {
                                Log.d(TAG, document.getId() + " => " + document.getData());
                                RatingPlace place = document.toObject(RatingPlace.class);
                                placeList.add(place);
                            }
                        }
                        ratingListAdapter.setRatingList(ratingList, placeList);
                    }
                });
    }

//    public void readRatingListBySpecificPlace(RatingPlace place) {
//        placeList.clear();
//        placeList.add(place);
//
//        Query query = db.collection("ratings").whereEqualTo("placeid", place.getPlaceid());
//        readRatingList(query);
//    }

    private void readRatingList(Query query) {
        // Listen for realtime updates of the ratings
        query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value,
                                @Nullable FirebaseFirestoreException error) {
                if(error != null) {
                    Log.w(TAG, "Listen failed.", error);
                    return;
                }

                ratingList.clear();
                for(QueryDocumentSnapshot document: value) {
                    if (document != null) {
                        Log.d(TAG, document.getId() + " => " + document.getData());
                        Rating rating = document.toObject(Rating.class);
                        ratingList.add(rating);
                    }
                }
                ratingListAdapter.setRatingList(ratingList, placeList);
            }
        });
    }

    private void readRatingListByCurrentLocation(LatLng currentLocation) {

        placeList.clear();

        //find places to be matched with user's current location
        Log.d(TAG, "current location's latitude: " + currentLocation.latitude + ", longitude: " + currentLocation.longitude);

        final GeoLocation center = new GeoLocation(currentLocation.latitude, currentLocation.longitude);
        final double radiusInMeter = 5 * 1000; //find places within 5km

        List<GeoQueryBounds> bounds = GeoFireUtils.getGeoHashQueryBounds(center, radiusInMeter);
        final List<Task<QuerySnapshot>> tasks = new ArrayList<>();
        for (GeoQueryBounds b : bounds) {
            Query q = db.collection("places")
                    .orderBy("geohash")
                    .startAt(b.startHash)
                    .endAt(b.endHash);

            tasks.add(q.get());

            Log.d(TAG, "tasks.added");
        }

        List<String> curLocPlaceIds = new ArrayList<>();

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
                                    curLocPlaceIds.add(place.getPlaceid());
                                    Log.d(TAG, "placeList: "+ place.getName());
                                }
                            }
                        }

                        // rating list matched with places

                        Query query = db.collection("ratings").whereIn("placeid", curLocPlaceIds);
                        readRatingList(query);
                    }
                });
    }
}