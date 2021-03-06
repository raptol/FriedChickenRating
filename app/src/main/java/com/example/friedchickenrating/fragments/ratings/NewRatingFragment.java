package com.example.friedchickenrating.fragments.ratings;

import static android.app.Activity.RESULT_OK;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;

import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.Toast;

import com.example.friedchickenrating.MainActivity;
import com.example.friedchickenrating.R;
import com.example.friedchickenrating.UserProfileActivity;
import com.example.friedchickenrating.databinding.FragmentNewRatingBinding;
import com.example.friedchickenrating.fragments.maps.MapsFragment;
import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewRatingFragment extends Fragment {

    private RatingViewModel ratingViewModel;
    private FragmentNewRatingBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private RatingPlace placeData;
    private Boolean isEditing = false;

    private static final String TAG = NewRatingFragment.class.getSimpleName();

    private ImageView imgViewPicture;
    private Uri filePath;
    private String fileName;
    private boolean hasImageToUpload = false;
    static final int REQUEST_IMAGE_SELECT = 0;
    static final int REQUEST_IMAGE_CAPTURE = 1;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentNewRatingBinding.inflate(inflater, container, false);
        View rootView = binding.getRoot();

        ratingViewModel = new ViewModelProvider(requireActivity()).get(RatingViewModel.class);

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        imgViewPicture = binding.imgViewPicture;

        placeData = new RatingPlace();
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        //Register result listener to get place info from map
        getParentFragmentManager().setFragmentResultListener("passByMapPlace", this,
                new FragmentResultListener() {
                    @Override
                    public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                        isEditing = false;

                        ratingViewModel.setMapRequestCode(0); //initialize

                        Rating newRating = new Rating(); //initialize
                        ratingViewModel.setSelectedRating(newRating);//Rating initialize

                        getValuesFromOtherFragment(result);
                    }
                });

        //Register result listener to get place info from bottom sheet
        getParentFragmentManager().setFragmentResultListener("passByBottomSheet", this,
                new FragmentResultListener() {
                    @Override
                    public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                        isEditing = false;

                        ratingViewModel.setMapRequestCode(0); //initialize

                        String placeId = result.getString("placeId");
                        String placeName = result.getString("placeName");
                        Double latitude = result.getDouble("latitude");
                        Double longitude = result.getDouble("longitude");
                        String region = result.getString("region");
                        Log.d(TAG, "ResultListener, latitude: " + latitude + ", longitude: " + longitude);
                        Log.d(TAG, "ResultListener, placeId: " + placeId + ", placeName: " + placeName);
                        Log.d(TAG, "ResultListener, region: " + region );

                        placeData.setPlaceid(placeId);
                        if(placeName != null)
                            placeData.setName(placeName.trim().replaceAll("\\s+", " "));
                        placeData.setLatitude(latitude);
                        placeData.setLongitude(longitude);
                        placeData.setLatitude(latitude);
                        placeData.setLongitude(longitude);
                        placeData.setGeohash(
                                GeoFireUtils.getGeoHashForLocation(
                                        new GeoLocation(latitude, longitude)));
                        placeData.setRegion(region);

                        binding.newRatingPlaceName.setText(placeData.getName());
                        binding.newRatingRegion.setText(placeData.getRegion());

                        getChildFragmentManager().popBackStack();
                    }
                });

        //Register result listener to get rating info passed by view rating for Editing
        getParentFragmentManager().setFragmentResultListener("passByViewRating", this,
                new FragmentResultListener() {
                    @Override
                    public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                        isEditing = true;

                        getValuesFromOtherFragment(result);

                        //Change the title of action bar to Edit Rating
                        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(getString(R.string.menu_editRating));
                    }
                });

        // Taking picture event handler for profile image
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

        //event handler for open map button
        binding.btnOpenMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //store data before switching from new rating to map
                String docId = ""; // New rating mode
                if(isEditing) { // Edit rating mode
                    docId = ratingViewModel.getSelectedRating().getValue().getId();
                }
                Rating saveRatingData = new Rating(docId,
                        binding.newRatingTitle.getText().toString(),
                        binding.newRatingChickenType.getText().toString(),
                        placeData.getPlaceid(),
                        user.getUid(),
                        binding.newRatingOtherItems.getText().toString(),
                        binding.newRatingNotes.getText().toString(),
                        new HashMap<String, Object>(),
                        binding.ratingBarFlavor.getRating(),
                        binding.ratingBarCrunch.getRating(),
                        binding.ratingBarSpiciness.getRating(),
                        binding.ratingBarPortion.getRating(),
                        binding.ratingBarPrice.getRating(),
                        binding.ratingBarOverall.getRating(),
                        Timestamp.now());

                ratingViewModel.setSelectedRating(saveRatingData);
                ratingViewModel.setSelectedRatingImage(binding.imgViewPicture);
                ratingViewModel.setSelectedRatingImageFilePath(filePath);
                ratingViewModel.setMapRequestCode(MapsFragment.REQUEST_MAP_PLACE_FOR_ADD_RATING);

                NavHostFragment.findNavController(NewRatingFragment.this)
                        .navigate(R.id.action_nav_newRating_to_nav_maps);

            }
        });

        //event handler for ratingBar
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
                    if(ratingBars != null) {
                        float sum = 0;
                        int countWithRatingValue = 0;
                        for (int i = 0; i < ratingBars.size(); i++) {
                            if(ratingBars.get(i).getRating() > 0) {
                                sum += ratingBars.get(i).getRating();
                                countWithRatingValue++;
                            }
                        }

                        float overallByAverage = 0;
                        if(sum > 0 && countWithRatingValue > 0) {
                            overallByAverage = sum / countWithRatingValue;

                            binding.ratingBarOverall.setRating(overallByAverage);
                        }
                    }
                }
            });
        }

        //event handler for done button
        binding.btnDoneNewRating.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(imgViewPicture.getDrawable() == null) {
                    Toast.makeText(getContext(), "Please upload a picture of chicken menu.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if( binding.newRatingPlaceName.getText().toString().trim().isEmpty()) {
                    Toast.makeText(getContext(), "Please enter chicken restaurant name.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if(placeData != null && placeData.getPlaceid() == null) {
                    Toast.makeText(getContext(), "Please search and click chicken restaurant name in the map.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if(binding.newRatingTitle.getText().toString().trim().isEmpty()) {
                    Toast.makeText(getContext(), "Please enter title of chicken menu.", Toast.LENGTH_SHORT).show();
                    return;
                }

                //upload image to Google Firebase Storage
                Map<String, Object> photoValues = null;
                if(hasImageToUpload) {
                    Date now = new Date();
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss");
                    fileName = formatter.format(now) + ".jpg";

                    formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String photoDateTime = formatter.format(now);

                    Photo photo = new Photo(fileName, photoDateTime);
                    photoValues = photo.toMap();

                }else {  // get value from edit if the file is not updated
                    Rating recoverRatingData = ratingViewModel.getSelectedRating().getValue();
                    if(recoverRatingData != null) {
                        photoValues = recoverRatingData.getPictures();
                    }
                }

                //add new rating to Firestore DB
                String ratingsDocId;
                if(isEditing == false) {
                    ratingsDocId = db.collection("ratings").document().getId();
                } else {
                    ratingsDocId = ratingViewModel.getSelectedRating().getValue().getId();
                }

                Rating newRatingData = new Rating(ratingsDocId,
                                            binding.newRatingTitle.getText().toString(),
                                            binding.newRatingChickenType.getText().toString(),
                                            placeData.getPlaceid(),
                                            user.getUid(),
                                            binding.newRatingOtherItems.getText().toString(),
                                            binding.newRatingNotes.getText().toString(),
                                            photoValues,
                                            binding.ratingBarFlavor.getRating(),
                                            binding.ratingBarCrunch.getRating(),
                                            binding.ratingBarSpiciness.getRating(),
                                            binding.ratingBarPortion.getRating(),
                                            binding.ratingBarPrice.getRating(),
                                            binding.ratingBarOverall.getRating(),
                                            Timestamp.now());

                db.collection("ratings").document(ratingsDocId)
                        .set(newRatingData)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                Log.d(TAG, "New rating was successfully saved!");
                                Log.d(TAG, "Document ID:" + ratingsDocId);
                                Toast.makeText(getContext(), "add new rating success.", Toast.LENGTH_SHORT).show();

                                //add new place to Firestore DB
                                if(placeData.getPlaceid() != null) {
                                    db.collection("places").document(placeData.getPlaceid())
                                            .set(placeData)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void unused) {
                                                    Log.d(TAG, "New place was successfully saved!");
                                                    Log.d(TAG, "Document ID:" + placeData.getPlaceid());
                                                    Toast.makeText(getContext(), "add new place success.", Toast.LENGTH_SHORT).show();

                                                    if(hasImageToUpload) {
                                                        if(isEditing) {
                                                            //delete previous file
                                                            Map<String, Object> photoValues = null;
                                                            Rating recoverRatingData = ratingViewModel.getSelectedRating().getValue();
                                                            if(recoverRatingData != null) {
                                                                photoValues = recoverRatingData.getPictures();
                                                                if(photoValues != null && !photoValues.isEmpty()) {
                                                                    Log.d(TAG, "deleted filename: " + photoValues.get("filename").toString());
                                                                    deletePreviousFileFromFirebaseStorage(photoValues.get("filename").toString());
                                                                }
                                                            }
                                                        }

                                                        uploadImageToFirebaseStorage();
                                                    } else {
                                                        NavHostFragment.findNavController(NewRatingFragment.this)
                                                                .navigate(R.id.action_nav_newRating_to_nav_ratings);
                                                    }
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Log.w(TAG, "Error saving data", e);
                                                    Toast.makeText(getContext(), "add new place error.", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "Error saving data", e);
                                Toast.makeText(getContext(), "add new rating error.", Toast.LENGTH_SHORT).show();
                            }
                        });

            }
        });

        FloatingActionButton fab = getActivity().findViewById(R.id.fab);
        if(fab != null) {
            fab.setVisibility(View.INVISIBLE);
        }
    }

    private void getValuesFromOtherFragment(Bundle result) {
        ratingViewModel.setMapRequestCode(0); //initialize

        String placeId = result.getString("placeId");
        String placeName = result.getString("placeName");
        Double latitude = result.getDouble("latitude");
        Double longitude = result.getDouble("longitude");
        String region = result.getString("region");
        Log.d(TAG, "ResultListener, latitude: " + latitude + ", longitude: " + longitude);
        Log.d(TAG, "ResultListener, placeId: " + placeId + ", placeName: " + placeName);
        Log.d(TAG, "ResultListener, region: " + region );

        placeData.setPlaceid(placeId);
        if(placeName != null)
            placeData.setName(placeName.trim().replaceAll("\\s+", " "));
        placeData.setLatitude(latitude);
        placeData.setLongitude(longitude);
        placeData.setLatitude(latitude);
        placeData.setLongitude(longitude);
        placeData.setGeohash(
                GeoFireUtils.getGeoHashForLocation(
                        new GeoLocation(latitude, longitude)));
        placeData.setRegion(region);

        //recover stored data before switching from new rating to map
        filePath = ratingViewModel.getSelectedRatingImageFilePath().getValue();
        Log.d(TAG, "filePath==> " + filePath);

        ImageView tempPhoto = ratingViewModel.getSelectedRatingImage().getValue();
        if(tempPhoto != null) {
            BitmapDrawable tempPhotoDrawable = (BitmapDrawable) tempPhoto.getDrawable();
            if (tempPhotoDrawable != null) {
                Bitmap bitmap = tempPhotoDrawable.getBitmap();
                binding.imgViewPicture.setImageBitmap(bitmap);
                binding.imgViewPicture.invalidate();

                if(isEditing == false) {
                    hasImageToUpload = true;
                }
            }
        }

        binding.newRatingPlaceName.setText(placeData.getName());
        binding.newRatingRegion.setText(placeData.getRegion());

        Rating recoverRatingData = ratingViewModel.getSelectedRating().getValue();
        if(recoverRatingData != null) {
            binding.newRatingTitle.setText(recoverRatingData.getTitle());
            binding.newRatingChickenType.setText(recoverRatingData.getType());
            binding.newRatingOtherItems.setText(recoverRatingData.getOtheritems());
            binding.newRatingNotes.setText(recoverRatingData.getNotes());
            binding.ratingBarFlavor.setRating(recoverRatingData.getStarflavor());
            binding.ratingBarCrunch.setRating(recoverRatingData.getStarcrunch());
            binding.ratingBarSpiciness.setRating(recoverRatingData.getStarspiciness());
            binding.ratingBarPortion.setRating(recoverRatingData.getStarportion());
            binding.ratingBarPrice.setRating(recoverRatingData.getStarprice());
            binding.ratingBarOverall.setRating(recoverRatingData.getStaroverall());
        }

        getChildFragmentManager().popBackStack();
    }

    @Override
    public void onStop() {
        FloatingActionButton fab = getActivity().findViewById(R.id.fab);
        fab.setVisibility(View.VISIBLE);
        super.onStop();
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
                        this.getContext().getContentResolver(), filePath);
                imgViewPicture.setImageBitmap(imageBitmap);
                hasImageToUpload = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void deletePreviousFileFromFirebaseStorage(String previousFile) {
        if(previousFile != null && !previousFile.isEmpty()) {
            final ProgressDialog progressDialog = new ProgressDialog(this.getContext());
            progressDialog.setTitle("is deleting previous image...");
            progressDialog.show();

            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageReference = storage.getReference().child("images/" + previousFile);

            // Delete the file
            storageReference.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    progressDialog.dismiss();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    progressDialog.dismiss();
                }
            });
        }
    }

    private void uploadImageToFirebaseStorage() {
        Log.d(TAG, "hasImageToUpload ==>" + hasImageToUpload);
        if(hasImageToUpload) {
            final ProgressDialog progressDialog = new ProgressDialog(this.getContext());
            progressDialog.setTitle("is uploading...");
            progressDialog.show();

            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageReference = storage.getReference().child("images/" + fileName);

            Log.d(TAG, "filePath ==>" + filePath);
            if(filePath != null) { // upload from choosing image
                storageReference.putFile(filePath)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                progressDialog.dismiss();

                                NavHostFragment.findNavController(NewRatingFragment.this)
                                        .navigate(R.id.action_nav_newRating_to_nav_ratings);
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

                                NavHostFragment.findNavController(NewRatingFragment.this)
                                        .navigate(R.id.action_nav_newRating_to_nav_ratings);
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
            Toast.makeText(this.getContext(), "Select image first.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}