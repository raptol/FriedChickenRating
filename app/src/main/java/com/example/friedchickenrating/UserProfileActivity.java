package com.example.friedchickenrating;

import static android.icu.lang.UCharacter.GraphemeClusterBreak.L;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.friedchickenrating.databinding.ActivityUserProfileBinding;
import com.example.friedchickenrating.fragments.ratings.NewRatingFragment;
import com.example.friedchickenrating.fragments.ratings.Photo;
import com.example.friedchickenrating.fragments.ratings.Rating;
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
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class UserProfileActivity extends AppCompatActivity {

    private ActivityUserProfileBinding binding;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private static final String TAG = "UserProfile";

    private RatingPlace placeData;
    private User userData;

    private List<String> backgroundCultureValues; // for spinner
    private static final String SPINNER_CHOOSE_MESSAGE ="Choose one.";

    private ImageView imgViewPicture;
    private Uri filePath;
    private String fileName;
    private boolean hasImageToUpload = false;
    static final int REQUEST_IMAGE_SELECT = 0;
    static final int REQUEST_IMAGE_CAPTURE = 1;

    private float savePreviousRatingValue;

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

        // If background data not exists, load data into Firestore DB
        db.collection("backgrounds")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if(task.getResult().size() == 0) {
                                //load initial data from file
                                List<String> backgroundList =
                                        Arrays.asList(getResources().getStringArray(R.array.backgrounds));

                                //insert initial data into Firestore DB
                                //Batch write
                                if(backgroundList.size() > 0) {
                                    backgroundCultureValues.clear();
                                    backgroundCultureValues.add(SPINNER_CHOOSE_MESSAGE);

                                    WriteBatch batch = db.batch();
                                    for (int i = 0; i < backgroundList.size(); i++) {
                                        String docId = db.collection("backgrounds").document().getId();
                                        Background newBackgroundData = new Background(docId, backgroundList.get(i));
                                        batch.set(db.collection("backgrounds").document(docId), newBackgroundData);

                                        backgroundCultureValues.add(newBackgroundData.getName());
                                    }
                                    // Commit the batch
                                    batch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            backgroundAdapter.notifyDataSetChanged();
                                            Log.d(TAG, "The background initial data was saved successfully.");
                                        }
                                    });
                                }
                            } else {
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
                                                for (QueryDocumentSnapshot document : value) {
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
                            }
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
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

                        //wait for filling spinner
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                binding.spnBackground.setSelection(backgroundAdapter.getPosition(userData.getBackground()));
                            }
                        }, 200);

                        binding.ratingBarFlavor.setRating(userData.getPreferflavor());
                        binding.ratingBarCrunch.setRating(userData.getPrefercrunch());
                        binding.ratingBarSpiciness.setRating(userData.getPreferspiciness());
                        binding.ratingBarPortion.setRating(userData.getPreferportion());
                        binding.ratingBarPrice.setRating(userData.getPreferprice());

                        Log.d(TAG, "user name: " + userData.getName() + ", user email: " + userData.getEmail());

                        // download and display images
                        Map<String, Object> pictures = userData.getPictures();
                        if(pictures != null) {
                            String filename = String.valueOf(pictures.get("filename"));
                            Log.d(TAG, "filename: " + filename);

                            if (filename != null && !filename.isEmpty()) {
                                long size;
                                final FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
                                StorageReference storageReference
                                        = firebaseStorage.getReference().child("images").child(filename);
                                storageReference.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Uri> task) {
                                        if (task.isSuccessful()) {

                                            if (getApplicationContext() != null) {
                                                Glide.with(getApplicationContext())
                                                        .load(task.getResult())
                                                        .into(binding.imgViewPicture);

                                                binding.imgViewPicture.invalidate();
                                            }
                                        } else {
                                            Toast.makeText(getApplicationContext(),
                                                    "Fail to load image", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                            }
                        }
                    } else {
                        Log.d(TAG, "No such user data");
                    }
                }
            }
        });

        // Taking picture event handler for profile image
        imgViewPicture = binding.imgViewPicture;
        binding.btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //By taking a picture
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        });

        // Choosing picture event handler for profile image
        binding.btnPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //By choosing a file
                Intent choosingPictureIntent = new Intent();
                choosingPictureIntent.setType("image/*");
                choosingPictureIntent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(choosingPictureIntent,
                        "Select a image."), REQUEST_IMAGE_SELECT);
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

            // format value to upload image to Google Firebase Storage
            Map<String, Object> photoValues = null;
            if(hasImageToUpload) {
                Date now = new Date();
                SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss");
                fileName = formatter.format(now) + ".jpg";

                formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String photoDateTime = formatter.format(now);

                Photo photo = new Photo(fileName, photoDateTime);
                photoValues = photo.toMap();
            }else {
                photoValues = userData.getPictures();
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
                                        userData.getSignup(),
                                        photoValues
                    );

            db.collection("users").document(user.getUid())
                    .set(updUserData)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "User profile was successfully saved!");

                            if(hasImageToUpload) {
                                uploadImageToFirebaseStorage();
                            } else {
                                startActivity((new Intent(UserProfileActivity.this, MainActivity.class)));
                                finish();
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error saving data", e);
                        }
                    });

            Toast.makeText(getApplicationContext(), "Update user profile success.", Toast.LENGTH_SHORT).show();
        });

        binding.btnClose.setOnClickListener((View view) -> {
            startActivity((new Intent(UserProfileActivity.this, MainActivity.class)));
            finish();
        });

        //Check rating value for priority
        List<RatingBar> ratingBars = new ArrayList<>();
        ratingBars.add(binding.ratingBarFlavor);
        ratingBars.add(binding.ratingBarCrunch);
        ratingBars.add(binding.ratingBarSpiciness);
        ratingBars.add(binding.ratingBarPortion);
        ratingBars.add(binding.ratingBarPrice);
        Log.d(TAG, "ratingBars.size(): " + ratingBars.size());

        for(int i = 0; i < ratingBars.size(); i++) {
            ratingBars.get(i).setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
                @Override
                public void onRatingChanged(RatingBar ratingBar, float v, boolean b) {
                    Log.d(TAG, "curRatingValue: " + v);
                    Log.d(TAG, "ratingBar value: " + ratingBar.getRating());

                    if(b == true && checkDuplicateRating(ratingBars, ratingBar, v)) {
                        //alert
                        Toast.makeText(getApplicationContext(),
                                "Please select different value for priority.", Toast.LENGTH_SHORT).show();

                        //roll back into previous rating value
                        ratingBar.setRating(savePreviousRatingValue);
                    }
                }
            });

            ratingBars.get(i).setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    savePreviousRatingValue = ((RatingBar)view).getRating();
                    Log.d(TAG, "prevRatingValue: " + savePreviousRatingValue);

                    return false;
                }
            });
        }
    }

    private boolean checkDuplicateRating(List<RatingBar> ratingBars, RatingBar curRatingBar, float rating) {
        boolean result = false;

        if(ratingBars != null) {
            for (int i = 0; i < ratingBars.size(); i++) {
                if ( !ratingBars.get(i).equals(curRatingBar) &&
                        ratingBars.get(i).getRating() == rating) {
                    result = true;
                    break;
                }
            }
        }

        return result;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        //By taking picture
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            if(data != null) {
                Log.d(TAG, "uri:" + String.valueOf(filePath));

                Bitmap imageBitmap = (Bitmap) data.getExtras().get("data");
                imgViewPicture.setImageBitmap(imageBitmap);
                hasImageToUpload = true;
            }

        }

        //By choosing file
        if (requestCode == REQUEST_IMAGE_SELECT && resultCode == RESULT_OK) {
            filePath = data.getData();
            Log.d(TAG, "uri:" + String.valueOf(filePath));

            try {
                Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(
                        this.getApplicationContext().getContentResolver(), filePath);
                imgViewPicture.setImageBitmap(imageBitmap);
                hasImageToUpload = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void uploadImageToFirebaseStorage() {
        if(hasImageToUpload) {
            final ProgressDialog progressDialog = new ProgressDialog(UserProfileActivity.this);
            progressDialog.setTitle("is uploading...");
            progressDialog.show();

            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageReference = storage.getReference().child("images/" + fileName);

            if(filePath != null) { // upload from choosing image
                storageReference.putFile(filePath)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                progressDialog.dismiss();
                                startActivity((new Intent(UserProfileActivity.this, MainActivity.class)));
                                finish();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                progressDialog.dismiss();
                            }
                        })
                        .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {

                            }
                        });
            }else { // upload from taking picture
                imgViewPicture.setDrawingCacheEnabled(true);
                imgViewPicture.buildDrawingCache();
                Bitmap bitmap = ((BitmapDrawable)imgViewPicture.getDrawable()).getBitmap();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                byte[] data = outputStream.toByteArray();

                storageReference.putBytes(data)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                progressDialog.dismiss();

                                startActivity((new Intent(UserProfileActivity.this, MainActivity.class)));
                                finish();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                progressDialog.dismiss();
                            }
                        })
                        .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {

                            }
                        });
            }
        } else {
            Toast.makeText(this.getApplicationContext(), "Select image first.", Toast.LENGTH_SHORT).show();
        }
    }

}