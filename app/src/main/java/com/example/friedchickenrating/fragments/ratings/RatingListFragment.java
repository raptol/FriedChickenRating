package com.example.friedchickenrating.fragments.ratings;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.example.friedchickenrating.R;
import com.example.friedchickenrating.User;
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
import com.google.firebase.firestore.DocumentReference;
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

    private User loginUser;
    private List<Rating> ratingList;
    private List<RatingPlace> placeList;
    private RatingListAdapter ratingListAdapter;

    private static final String TAG = RatingListFragment.class.getSimpleName();

    private static final int SORT_OPTION_LOCATION = 1;
    private static final int SORT_OPTION_HIGH_STARS = 2;
    private static final int SORT_OPTION_LATEST = 3;
    private static final int SORT_OPTION_RELEVANT = 4;
    private static final int SORT_OPTION_MY_RATING = 5;
    private static final int SORT_OPTION_MY_FLAVOR = 6;
    private static final int SORT_OPTION_MY_CULTURE = 7;
    private static final int SORT_OPTION_MY_HOMETOWN = 8;

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

        loginUser = new User();
        ratingList = new ArrayList<>();
        placeList = new ArrayList<>();

//        int orientation = getResources().getConfiguration().orientation;
//        int gridCount;
//        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
//            gridCount = 1;
//        } else {
//            gridCount = 2;
//        }

        ratingListAdapter = new RatingListAdapter(ratingList, placeList);
        ratingListAdapter.setListener(this);

        binding.recyclerViewRatingList.setLayoutManager(new GridLayoutManager(this.getContext(), 1));
        binding.recyclerViewRatingList.setAdapter(ratingListAdapter);

        //display rating list
        displayRatingList(SORT_OPTION_LATEST); //default sorting option: latest

        //Get Login User info
        readLoginUserInfo();

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

        binding.btnSortMyFlavor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayRatingList(SORT_OPTION_MY_FLAVOR);
            }
        });

        binding.btnSortMyCulture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayRatingList(SORT_OPTION_MY_CULTURE);
            }
        });

        binding.btnSortMyHometown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayRatingList(SORT_OPTION_MY_HOMETOWN);
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
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {
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
                        .orderBy("placeid")
                        .orderBy("title");
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

            case SORT_OPTION_MY_FLAVOR:
                AbstractMap.SimpleEntry<String, Float> topPriorityPreference = getTopPriorityFactor();
                if(topPriorityPreference != null) {
                    query = db.collection("ratings")
                            .whereGreaterThanOrEqualTo(
                                    convertKeyPrefix(topPriorityPreference.getKey()),
                                    topPriorityPreference.getValue());
                    readPlaceList();
                    //readRatingListByMyFactor(query);
                    readRatingList(query);
                }
                break;

            case SORT_OPTION_MY_CULTURE:
                query = db.collection("users")
                            .whereEqualTo("background", loginUser.getBackground());
                readPlaceList();
                readRatingListByMyFactor(query);
                break;

            case SORT_OPTION_MY_HOMETOWN:
                query = db.collection("users")
                        .whereEqualTo("hometown", loginUser.getHometown());
                readPlaceList();
                readRatingListByMyFactor(query);
                break;

            default: // by location
                readRatingListByCurrentLocation();
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

                Log.d(TAG, "excute fetch db, value.size: " + value.size());

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

    private void readRatingListByCurrentLocation() {

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
        if(lastUserKnownLocation != null) {
            userLocation = new LatLng(lastUserKnownLocation.getLatitude(), lastUserKnownLocation.getLongitude());

            placeList.clear();

            //find places to be matched with user's current location
            Log.d(TAG, "current location's latitude: " + userLocation.latitude + ", longitude: " + userLocation.longitude);

            final GeoLocation center = new GeoLocation(userLocation.latitude, userLocation.longitude);
            final double radiusInMeter = 50 * 1000; //find places within 50km

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
                                        Log.d(TAG, "placeList: " + place.getName());
                                    }
                                }
                            }

                            // rating list matched with places
                            if(curLocPlaceIds.size() > 0) {
                                Query query = db.collection("ratings").whereIn("placeid", curLocPlaceIds);
                                readRatingList(query);
                            }
                        }
                    });
        }
    }

    private void readRatingListByMyFactor(Query query) {

        //Get user list with my factor
        List<String> sameFactorUserIds = new ArrayList<>();

        query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value,
                                @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Log.w(TAG, "Listen failed.", error);
                    return;
                }

                Log.d(TAG, "excute fetch db, value.size: " + value.size());

                sameFactorUserIds.clear();
                for (QueryDocumentSnapshot document : value) {
                    if (document != null) {
                        Log.d(TAG, document.getId() + " => " + document.getData());
                        User sameFactorUser = document.toObject(User.class);

                        //except for login user
                        if(loginUser != null && !loginUser.getUid().equals(sameFactorUser.getUid())) {
                            Log.d(TAG, "sameFactorUser: " + sameFactorUser.getName());
                            sameFactorUserIds.add(sameFactorUser.getUid());
                        }
                    }
                }

                // rating list matched with places
                Log.d(TAG, "userList.size: " + sameFactorUserIds.size());
                if(sameFactorUserIds.size() > 0) {
                    Query ratingQuery = db.collection("ratings").whereIn("userid", sameFactorUserIds);
                    readRatingList(ratingQuery);
                }else {
                    placeList.clear();
                    ratingList.clear();
                    ratingListAdapter.setRatingList(ratingList, placeList);
                }
            }
        });
    }

    private void readLoginUserInfo() {

        //Get login user info
        db.collection("users").document(user.getUid())
                .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if(document.exists()) {
                        loginUser = document.toObject(User.class);

                        Log.d(TAG, "loginUser: " + loginUser.getName());

                    }else {
                        Log.d(TAG, "No such user");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });
    }

    private AbstractMap.SimpleEntry<String, Float> getTopPriorityFactor() {
        AbstractMap.SimpleEntry<String, Float> topPriorityPreference = null;
        float maxValue = 0;

        if(loginUser.getPrefercrunch() > maxValue) {
            maxValue = loginUser.getPrefercrunch();
            topPriorityPreference = new AbstractMap.SimpleEntry<>("prefercrunch", maxValue);
        }
        if(loginUser.getPreferflavor() > maxValue) {
            maxValue = loginUser.getPreferflavor();
            topPriorityPreference = new AbstractMap.SimpleEntry<>("preferflavor", maxValue);
        }
        if(loginUser.getPreferportion() > maxValue) {
            maxValue = loginUser.getPreferportion();
            topPriorityPreference = new AbstractMap.SimpleEntry<>("preferportion", maxValue);
        }
        if(loginUser.getPreferprice() > maxValue) {
            maxValue = loginUser.getPreferprice();
            topPriorityPreference = new AbstractMap.SimpleEntry<>("preferprice", maxValue);
        }
        if(loginUser.getPreferspiciness() > maxValue) {
            maxValue = loginUser.getPreferspiciness();
            topPriorityPreference = new AbstractMap.SimpleEntry<>("preferspiciness", maxValue);
        }

        return topPriorityPreference;
    }

    private String convertKeyPrefix(String key) {
        String prefixUserPreference = "prefer";
        String prefixRatingFactor = "star";
        String result = "";

        result = prefixRatingFactor + key.substring(prefixUserPreference.length(), key.length());

        Log.d(TAG, "==> Factor key: " + result);
        return result;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}