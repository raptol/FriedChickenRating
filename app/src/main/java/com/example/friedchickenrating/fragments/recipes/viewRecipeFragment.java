package com.example.friedchickenrating.fragments.recipes;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.friedchickenrating.databinding.FragmentViewRecipeBinding;
import com.example.friedchickenrating.fragments.ratings.ViewRatingFragment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class viewRecipeFragment extends Fragment {
    private RecipesViewModel recipeViewModel;
    private FragmentViewRecipeBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser user;

    private List<Recipe> recipeList;
    static final int REQUEST_MAP_PLACE_FOR_VIEW_RATING = 2;
    static final int REQUEST_BOTTOM_SHEET_FOR_VIEW_RATING = 2;

    private static final String TAG = ViewRatingFragment.class.getSimpleName();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        recipeViewModel = new ViewModelProvider(requireActivity()).get(RecipesViewModel.class);
        binding = FragmentViewRecipeBinding.inflate(inflater, container, false);
        View rootView = binding.getRoot();
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        recipeList = new ArrayList<>();

        Recipe curRecipe = recipeViewModel.getSelectedRecipe().getValue();

        Log.d(TAG, "curRecipe.id: " + curRecipe.getRecipeId());
        Log.d(TAG, "curRecipe.title: " + curRecipe.getRecipeTitle());

        // Listen for realtime updates of the places
        if(curRecipe != null) {
            db.collection("recipes").document(curRecipe.getRecipeId())
                    .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                        @Override
                        public void onEvent(@Nullable DocumentSnapshot value,
                                            @Nullable FirebaseFirestoreException error) {
                            if (error != null) {
                                Log.w(TAG, "Listen failed.", error);
                                return;
                            }

//                            selectedPlace = value.toObject(RatingPlace.class);

                            // download and display images
                            Map<String, Object> pictures = curRecipe.getPictures();
                            String filename = String.valueOf(pictures.get("filename"));
                            Log.d(TAG, "filename: " + filename);

                            if (!filename.isEmpty() && filename != null) {
                                long size;
                                final FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
                                StorageReference storageReference
                                        = firebaseStorage.getReference().child("images").child(filename);
                                storageReference.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Uri> task) {
                                        if (task.isSuccessful()) {
                                            Glide.with(getActivity())
                                                    .load(task.getResult())
                                                    .into(binding.imgViewRecipePicture);
                                            binding.imgViewRecipePicture.invalidate();
                                        } else {
                                            Toast.makeText(getContext(),
                                                    "Fail to load image", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                            }


                            binding.viewRecipeTitle.setText(curRecipe.getRecipeTitle());
                            binding.recipeIngredient1.setText(curRecipe.getRecipeIngredients());
                            binding.txtRecipeStep1.setText(curRecipe.getRecipeSteps());
                            binding.txtRecipeStep2.setText(curRecipe.getRecipeSteps());

                        }
                    });
        }

    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
