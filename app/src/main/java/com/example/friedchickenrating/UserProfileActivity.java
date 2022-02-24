package com.example.friedchickenrating;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
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
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;

public class UserProfileActivity extends AppCompatActivity {

    private ActivityUserProfileBinding binding;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private static final String TAG = "UserProfile";
    private User userData;

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
        String apiKey = getString(R.string.api_key);
        if(!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), apiKey);
        }

        // Initialize the AutocompleteSupportFragment.
        RatingPlace placeData = new RatingPlace();
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
            Double latitude = null;
            Double longitude = null;
            String geohash = null;

            if(!editTextHometownName.getText().toString().isEmpty()) {

                //hometown = placeData.getName();
                hometown = editTextHometownName.getText().toString().trim();
                latitude = placeData.getLatitude();
                longitude = placeData.getLongitude();
                geohash = GeoFireUtils.getGeoHashForLocation(
                                new GeoLocation(latitude, longitude));
            }

            //update user's info to Firestore DB
            User updUserData = new User(userData.getUid(), userData.getName(), userData.getEmail(),
                                        hometown,
                                        latitude,
                                        longitude,
                                        geohash,
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