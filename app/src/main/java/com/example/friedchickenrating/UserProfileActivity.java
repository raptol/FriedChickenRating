package com.example.friedchickenrating;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import com.example.friedchickenrating.databinding.ActivityUserProfileBinding;
import com.example.friedchickenrating.fragments.ratings.RatingPlace;
import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UserProfileActivity extends AppCompatActivity {

    private ActivityUserProfileBinding binding;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private static final String TAG = "UserProfile";

    private RatingPlace placeData;
    private User userData;

    private List<String> backgroundCultureValues; // for spinner
    private static final String SPINNER_CHOOSE_MESSAGE ="Choose one.";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        binding = ActivityUserProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        Bundle bundle = getIntent().getExtras();
        if(bundle != null) {
            Boolean isFirstLogin = bundle.getBoolean("isFirstLogin");
            if(isFirstLogin) {
                binding.txtWelcome.setVisibility(View.VISIBLE);
                binding.txtWelcomMessage.setVisibility(View.VISIBLE);
            }
        }

        // Initiate the SDK for Places
        String apiKey = getString(R.string.google_maps_key);
        if(!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), apiKey);
        }

        // Initialize the AutocompleteSupportFragment.
        placeData = new RatingPlace();
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.placename_autocomplete_fragment);
        autocompleteFragment.getView().findViewById(R.id.places_autocomplete_search_button).setVisibility(View.GONE);

        EditText editTextHometownName = (EditText) autocompleteFragment.getView().findViewById(R.id.places_autocomplete_search_input);
        editTextHometownName.setHint("Hometown name");
        editTextHometownName.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        //editTextHometownName.setBackgroundColor(Color.GRAY);

        //autocompleteFragment.setCountries("CA");
        autocompleteFragment.setPlaceFields(Arrays.asList(com.google.android.libraries.places.api.model.Place.Field.ID, com.google.android.libraries.places.api.model.Place.Field.NAME, com.google.android.libraries.places.api.model.Place.Field.LAT_LNG));
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull com.google.android.libraries.places.api.model.Place place) {
                Log.i(TAG, "Place: " + place.getName() + ", " + place.getId());
                placeData.setPlaceid(place.getId());
                placeData.setName(place.getName());
                placeData.setLatitude(place.getLatLng().latitude);
                placeData.setLongitude(place.getLatLng().longitude);
            }

            @Override
            public void onError(@NonNull Status status) {
                Log.i(TAG, "An error occurred: " + status);
            }
        });

        // setup Spinner for Background culture
        backgroundCultureValues = new ArrayList<>();
        ArrayAdapter backgroundAdapter = new ArrayAdapter(this,
                android.R.layout.simple_spinner_item, backgroundCultureValues);
        backgroundAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spnBackground.setAdapter(backgroundAdapter);

        // Listen for realtime updates of the backgrounds
        db.collection("backgrounds")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value,
                                        @Nullable FirebaseFirestoreException error) {
                        if(error != null) {
                            Log.w(TAG, "Listen failed.", error);
                            return;
                        }

                        backgroundCultureValues.clear();
                        backgroundCultureValues.add(SPINNER_CHOOSE_MESSAGE);
                        for(QueryDocumentSnapshot document: value) {
                            if (document != null) {
                                Log.d(TAG, document.getId() + " => " + document.getData());
                                Background background = document.toObject(Background.class);
                                backgroundCultureValues.add(background.getName());
                                Log.d(TAG, "backgroundCulture: " + background.getName());
                            }
                        }

                        backgroundAdapter.notifyDataSetChanged();
                    }
                });

        //get user from Firestore DB
        DocumentReference usersDbRef = db.collection("users").document(user.getUid());
        usersDbRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if(document.exists()) {
                        userData = document.toObject(User.class);

                        binding.editTxtName.setText(userData.getName());
                        binding.editTxtEmail.setText(userData.getEmail());
                        editTextHometownName.setText(userData.getHometown());
                        binding.spnBackground.setSelection(backgroundAdapter.getPosition(userData.getBackground()));

                        binding.ratingBarFlavor.setRating(userData.getPreferflavor());
                        binding.ratingBarCrunch.setRating(userData.getPrefercrunch());
                        binding.ratingBarSpiciness.setRating(userData.getPreferspiciness());
                        binding.ratingBarPortion.setRating(userData.getPreferportion());
                        binding.ratingBarPrice.setRating(userData.getPreferprice());

                        Log.d(TAG, "user name: " + userData.getName() + ", user email: " + userData.getEmail());
                    } else {
                        Log.d(TAG, "No such user data");
                    }
                }
            }
        });

        binding.btnSave.setOnClickListener((View view) -> {

            String hometown = null;
            Double latitude = 0.0;
            Double longitude = 0.0;
            String geohash = null;

            if(editTextHometownName.getText().toString().isEmpty()) {
                Toast.makeText(getApplicationContext(), "Please enter your hometown to help your tastes.", Toast.LENGTH_SHORT).show();
                return;
            } else {
                Log.d(TAG, "userData.getHometown(): " + userData.getHometown());

                hometown = editTextHometownName.getText().toString();
                if(placeData.getPlaceid() != null) {
                    latitude = placeData.getLatitude();
                    longitude = placeData.getLongitude();
                    geohash = GeoFireUtils.getGeoHashForLocation(new GeoLocation(latitude, longitude));
                } else {
                    latitude = userData.getLatitude();
                    longitude = userData.getLongitude();
                    geohash = userData.getGeohash();
                }
            }

            if(binding.spnBackground.getSelectedItem().toString().equals(SPINNER_CHOOSE_MESSAGE)) {
                Toast.makeText(getApplicationContext(), "Please choose your background culture to help your tastes.", Toast.LENGTH_SHORT).show();
                return;
            }

            //update user's info to Firestore DB
            User updUserData = new User(userData.getUid(), userData.getName(), userData.getEmail(),
                                        hometown,
                                        latitude,
                                        longitude,
                                        geohash,
                                        binding.spnBackground.getSelectedItem().toString(),
                                        binding.ratingBarFlavor.getRating(),
                                        binding.ratingBarCrunch.getRating(),
                                        binding.ratingBarSpiciness.getRating(),
                                        binding.ratingBarPortion.getRating(),
                                        binding.ratingBarPrice.getRating(),
                                        userData.getLastlogin(),
                                        userData.getSignup()
                    );

            db.collection("users").document(user.getUid())
                    .set(updUserData)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "User profile was successfully saved!");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error saving data", e);
                        }
                    });

            Toast.makeText(getApplicationContext(), "Update user profile success.", Toast.LENGTH_SHORT).show();
            startActivity((new Intent(UserProfileActivity.this, MainActivity.class)));
            finish();
        });

        binding.btnClose.setOnClickListener((View view) -> {
            startActivity((new Intent(UserProfileActivity.this, MainActivity.class)));
            finish();
        });
    }
}