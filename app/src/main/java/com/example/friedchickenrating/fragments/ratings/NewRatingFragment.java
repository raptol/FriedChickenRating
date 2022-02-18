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
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.friedchickenrating.R;
import com.example.friedchickenrating.databinding.FragmentNewRatingBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class NewRatingFragment extends Fragment {

    private RatingViewModel ratingViewModel;
    private FragmentNewRatingBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private ImageView imgViewNewPhoto;
    private Uri filePath;
    private String fileName;

    private static final String TAG = NewRatingFragment.class.getSimpleName();
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

        imgViewNewPhoto = binding.imgViewPicture;
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        Place placeData = new Place();

        //Register result listener to get place info from map
        getParentFragmentManager().setFragmentResultListener("requestMapPlaceInfo", this,
                new FragmentResultListener() {
                    @Override
                    public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                        String placeId = result.getString("placeId");
                        String placeName = result.getString("placeName");
                        String latitude = result.getString("latitude");
                        String longitude = result.getString("longitude");
                        Log.d(TAG, "ResultListener, latitude: " + latitude + ", longitude: " + longitude);
                        Log.d(TAG, "ResultListener, placeId: " + placeId + ", placeName: " + placeName);

                        placeData.setPlaceid(placeId);
                        placeData.setName(placeName);
                        placeData.setLatitude(latitude);
                        placeData.setLongitude(longitude);
                        placeData.setRegion("");

                        binding.newRatingPlaceName.setText(placeData.getName());
                        binding.newRatingRegion.setText(placeData.getRegion());

                        //recover stored data before switching from new rating to map
                        filePath = ratingViewModel.getSelectedRatingImageFilePath().getValue();
                        Log.d(TAG, "filePath==> " + filePath);

                        ImageView tempPhoto = ratingViewModel.getSelectedRatingImage().getValue();
                        BitmapDrawable drawable = (BitmapDrawable)tempPhoto.getDrawable();
                        Bitmap bitmap = drawable.getBitmap();
                        binding.imgViewPicture.setImageBitmap(bitmap);
                        binding.imgViewPicture.invalidate();

                        Rating recoverRatingData = ratingViewModel.getSelectedRating().getValue();
                        binding.newRatingTitle.setText(recoverRatingData.getTitle());
                        binding.newRatingChickenType.setText(recoverRatingData.getType());
                        binding.newRatingRegion.setText(recoverRatingData.getRegion());
                        binding.newRatingOtherItems.setText(recoverRatingData.getOtheritems());
                        binding.newRatingNotes.setText(recoverRatingData.getNotes());
                        binding.ratingBarFlavor.setRating(recoverRatingData.getStarflavor());
                        binding.ratingBarCrunch.setRating(recoverRatingData.getStarcrunch());
                        binding.ratingBarSpiciness.setRating(recoverRatingData.getStarspiciness());
                        binding.ratingBarPortion.setRating(recoverRatingData.getStarportion());
                        binding.ratingBarPrice.setRating(recoverRatingData.getStarprice());
                        binding.ratingBarOverall.setRating(recoverRatingData.getStaroverall());
                    }
                });

        //event handler for upload picture
        binding.btnUploadPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //By choosing file
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
                Rating saveRatingData = new Rating("",
                        binding.newRatingTitle.getText().toString(),
                        binding.newRatingChickenType.getText().toString(),
                        binding.newRatingRegion.getText().toString(),
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
                        binding.ratingBarOverall.getRating());

                ratingViewModel.setSelectedRating(saveRatingData);
                ratingViewModel.setSelectedRatingImage(binding.imgViewPicture);
                ratingViewModel.setSelectedRatingImageFilePath(filePath);
                NavHostFragment.findNavController(NewRatingFragment.this)
                        .navigate(R.id.action_nav_newRating_to_nav_maps);

            }
        });

        //event handler for done button
        binding.btnDoneNewRating.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //upload image to Google Firebase Storage
                uploadImageToFirebaseStorage();

                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date now = new Date();
                String photoDateTime = formatter.format(now);

                Photo photo = new Photo(fileName, photoDateTime);
                Map<String, Object> photoValues = photo.toMap();
                //Map<String, Object> picturesMap = new HashMap<>();
                //picturesMap.put("pictures", photoValues);

                //add new rating to Firestore DB
                String ratingsDocId = db.collection("ratings").document().getId();
                Rating newRatingData = new Rating(ratingsDocId,
                                            binding.newRatingTitle.getText().toString(),
                                            binding.newRatingChickenType.getText().toString(),
                                            binding.newRatingRegion.getText().toString(),
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
                                            binding.ratingBarOverall.getRating());

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

                NavHostFragment.findNavController(NewRatingFragment.this)
                        .navigate(R.id.action_nav_newRating_to_nav_ratings);
            }
        });

        FloatingActionButton fab = getActivity().findViewById(R.id.fab);
        fab.setVisibility(View.INVISIBLE);
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
//        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
//            Bundle extras = data.getExtras();
//            Bitmap imageBitmap = (Bitmap) extras.get("data");
//            imgViewNewPhoto.setImageBitmap(imageBitmap);
//        }

        //By choosing file
        if (requestCode == REQUEST_IMAGE_SELECT && resultCode == RESULT_OK) {
            filePath = data.getData();
            Log.d(TAG, "uri:" + String.valueOf(filePath));

            try {
                Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(
                        this.getContext().getContentResolver(), filePath);
                imgViewNewPhoto.setImageBitmap(imageBitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void uploadImageToFirebaseStorage() {
        if(filePath != null) {
            final ProgressDialog progressDialog = new ProgressDialog(this.getContext());
            progressDialog.setTitle("is uploading...");
            progressDialog.show();

            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss");
            Date now = new Date();
            fileName = formatter.format(now) + ".jpg";

            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageReference = storage.getReferenceFromUrl("gs://friedchickenrating.appspot.com/").child("images/" + fileName);
            storageReference.putFile(filePath)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            progressDialog.dismiss();
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
        } else {
            Toast.makeText(this.getContext(), "Select image first.", Toast.LENGTH_SHORT).show();
        }
    }
}