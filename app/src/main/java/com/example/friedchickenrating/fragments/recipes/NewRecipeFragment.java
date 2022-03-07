package com.example.friedchickenrating.fragments.recipes;

import static android.app.Activity.RESULT_OK;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.friedchickenrating.R;
import com.example.friedchickenrating.databinding.FragmentNewRecipeBinding;
import com.example.friedchickenrating.fragments.ratings.NewRatingFragment;
import com.example.friedchickenrating.fragments.ratings.Photo;
import com.example.friedchickenrating.fragments.ratings.Rating;
import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
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
import java.util.Map;
import java.util.Objects;

public class NewRecipeFragment extends Fragment {

    private RecipesViewModel recipesViewModel;
    private FragmentNewRecipeBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private ImageView imgViewNewPhoto;
    private Uri filePath;
    private String fileName;

    private Recipe recipeData;
    private Boolean isEditing = false;

    private static final String TAG = NewRecipeFragment.class.getSimpleName();
    static final int REQUEST_IMAGE_SELECT = 0;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_MAP_PLACE_FOR_ADD_RATING = 1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentNewRecipeBinding.inflate(inflater, container, false);
        View rootView = binding.getRoot();

        recipesViewModel = new ViewModelProvider(requireActivity()).get(RecipesViewModel.class);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recipeData = new Recipe();

        imgViewNewPhoto = binding.imgViewRecipePicture;
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        //Register result listener to get recipe info passed by view recipe for Editing
        getParentFragmentManager().setFragmentResultListener("passByViewRecipes", this,
                new FragmentResultListener() {
                    @Override
                    public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                        isEditing = true;

                        getValuesFromOtherFragment(result);

                        //Change the title of action bar to Edit Recipe
                        Objects.requireNonNull(((AppCompatActivity) getActivity()).getSupportActionBar()).setTitle(getString(R.string.menu_editRecipes));
                    }
                });

        //event handler for upload picture
        binding.btnUploadPic.setOnClickListener(new View.OnClickListener() {
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

        //event handler for done button
        binding.btnNewRecipeDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(imgViewNewPhoto.getDrawable() == null) {
                    Toast.makeText(getContext(), "Please upload a picture of chicken recipe.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if( binding.etNewRecipeName.getText().toString().trim().isEmpty()) {
                    Toast.makeText(getContext(), "Please enter chicken recipe name.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if(binding.etRecipeIngredient1.getText().toString().trim().isEmpty()) {
                    Toast.makeText(getContext(), "Please enter ingredients.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if(binding.etRecipeStep1.getText().toString().trim().isEmpty()) {
                    Toast.makeText(getContext(), "Please enter steps.", Toast.LENGTH_SHORT).show();
                    return;
                }

                //upload image to Google Firebase Storage
                Map<String, Object> photoValues = null;
                if(filePath != null) {
                    uploadImageToFirebaseStorage();

                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date now = new Date();
                    String photoDateTime = formatter.format(now);

                    Photo photo = new Photo(fileName, photoDateTime);
                    photoValues = photo.toMap();
                }else {  // get value from edit if the file is not updated
                    Recipe recoverRecipeData = recipesViewModel.getSelectedRecipe().getValue();
                    if(recoverRecipeData != null) {
                        photoValues = recoverRecipeData.getPictures();
                    }
                }

                //add new recipe to Firestore DB
                String recipeDocId;
                if(isEditing == false) {
                    recipeDocId = db.collection("recipes").document().getId();
                } else {
                    recipeDocId = recipesViewModel.getSelectedRecipe().getValue().getRecipeId();
                }

                Recipe newRecipeData = new Recipe(recipeDocId,
                        binding.etNewRecipeName.getText().toString(),
                        binding.etRecipeIngredient1.getText().toString(),
                        binding.etRecipeStep1.getText().toString(),
                        user.getUid(),
                        photoValues,
                        Timestamp.now());

                db.collection("recipes").document(recipeDocId)
                        .set(newRecipeData)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                Log.d(TAG, "New recipe was successfully saved!");
                                Log.d(TAG, "Document ID:" + recipeDocId);
                                Toast.makeText(getContext(), "add new recipe success.", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "Error saving data", e);
                                Toast.makeText(getContext(), "add new recipe error.", Toast.LENGTH_SHORT).show();
                            }
                        });

                //myAdapter.notifyItemRangeChanged()
                NavHostFragment.findNavController(NewRecipeFragment.this)
                        .navigate(R.id.action_nav_newRecipe_to_nav_recipes);
            }
        });

        FloatingActionButton fab = getActivity().findViewById(R.id.fab);
        if(fab != null) {
            fab.setVisibility(View.INVISIBLE);
        }
    }

    private void getValuesFromOtherFragment(Bundle result) {

//        recipeData = recipesViewModel.getSelectedRecipe().getValue();

        String recipeTitle = result.getString("recipeTitle");
        String ingredients = result.getString("ingredients");
        String steps = result.getString("steps");
//        String region = result.getString("region");

        recipeData.setRecipeTitle(recipeTitle);
        recipeData.setRecipeIngredients(ingredients);
        recipeData.setRecipeSteps(steps);


//        binding.etNewRecipeName.setText(recipeData.getRecipeTitle());
        //recover stored data before switching from new rating to map
        filePath = recipesViewModel.getSelectedRecipeImageFilePath().getValue();
        Log.d(TAG, "filePath==> " + filePath);

        ImageView tempPhoto = recipesViewModel.getSelectedRecipeImage().getValue();
        if(tempPhoto != null) {
            BitmapDrawable tempPhotoDrawable = (BitmapDrawable) tempPhoto.getDrawable();
            if (tempPhotoDrawable != null) {
                Bitmap bitmap = tempPhotoDrawable.getBitmap();
                binding.imgViewRecipePicture.setImageBitmap(bitmap);
                binding.imgViewRecipePicture.invalidate();
            }
        }

        Recipe recoverRecipeData = recipesViewModel.getSelectedRecipe().getValue();
        if(recoverRecipeData != null) {
            binding.etNewRecipeName.setText(recoverRecipeData.getRecipeTitle());
            binding.etRecipeIngredient1.setText(recoverRecipeData.getRecipeIngredients());
            binding.etRecipeStep1.setText(recoverRecipeData.getRecipeSteps());
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
            StorageReference storageReference = storage.getReference().child("images/" + fileName);
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
