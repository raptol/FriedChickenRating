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
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.example.friedchickenrating.R;
import com.example.friedchickenrating.databinding.FragmentViewRecipeBinding;
import com.example.friedchickenrating.fragments.ratings.ViewRatingFragment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
import java.util.Objects;

public class ViewRecipeFragment extends Fragment {
    private RecipesViewModel recipesViewModel;
    private FragmentViewRecipeBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser user;

    private List<Recipe> recipeList;

    private static final String TAG = ViewRatingFragment.class.getSimpleName();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        recipesViewModel = new ViewModelProvider(requireActivity()).get(RecipesViewModel.class);
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

        Recipe curRecipe = recipesViewModel.getSelectedRecipe().getValue();

        Log.d(TAG, "curRecipe.id: " + curRecipe.getRecipeId());
        Log.d(TAG, "curRecipe.title: " + curRecipe.getRecipeTitle());

        //Change the title of action bar to Edit Recipe
        Objects.requireNonNull(((AppCompatActivity) getActivity()).getSupportActionBar()).setTitle(getString(R.string.menu_viewRecipe));

        // Listen for realtime updates of the places
        if(curRecipe != null && curRecipe.getRecipeId() != null) {
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
//                            binding.txtRecipeStep2.setText(curRecipe.getRecipeSteps());
                        }
                    });
        }


        //If login user is user who created current rating, enable Edit button
        //else, disable Edit button
        if(user.getUid() != null && user.getUid().equals(curRecipe.getUserid())) {
            binding.btnEditRecipe.setVisibility(View.VISIBLE);
            binding.btnDeleteRecipe.setVisibility(View.VISIBLE);
        } else {
            binding.btnEditRecipe.setVisibility(View.INVISIBLE);
            binding.btnDeleteRecipe.setVisibility(View.INVISIBLE);
        }

        //event handler for edit button
        binding.btnEditRecipe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                Integer requestCode = ratingViewModel.getMapRequestCode().getValue();
                Bundle result = new Bundle();
                result.putString("recipeTitle", binding.viewRecipeTitle.toString());
                result.putString("ingredients", binding.recipeIngredient1.toString());
                result.putString("steps", binding.txtRecipeStep1.toString());
//                result.putDouble("steps", selectedPlace.getLatitude());


                recipesViewModel.setSelectedRecipe(curRecipe);
                recipesViewModel.setSelectedRecipeImage(binding.imgViewRecipePicture);

                getParentFragmentManager().setFragmentResult("passByViewRecipes", result);

                NavHostFragment.findNavController(ViewRecipeFragment.this)
                        .navigate(R.id.action_nav_viewRecipes_to_newRecipeFragment3);
            }
        });

        //event handler for delete button
        binding.btnDeleteRecipe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                db.collection("recipes").document(curRecipe.getRecipeId())
                        .delete()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                Log.d(TAG, "The recipe was successfully deleted!");
                                Toast.makeText(getContext(), "delete the recipe success.", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "Error deleting the recipe", e);
                                Toast.makeText(getContext(), "delete the recipe error.", Toast.LENGTH_SHORT).show();
                            }
                        });

                //myAdapter.notifyItemRangeChanged()
                NavHostFragment.findNavController(ViewRecipeFragment.this)
                        .navigate(R.id.action_nav_viewRecipes_to_nav_recipes);
            }
        });


    }







    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
