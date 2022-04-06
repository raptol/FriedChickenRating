package com.example.friedchickenrating.fragments.recipes;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.friedchickenrating.R;
import com.example.friedchickenrating.databinding.FragmentRecipesListBinding;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class RecipesListFragment extends Fragment implements RecipesListAdapter.ItemClickListener {

    private static RecipesListFragment instance = null;
    private RecipesViewModel recipesViewModel;
    private FragmentRecipesListBinding binding;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser user;

    private List<Recipe> recipeList;
    private RecipesListAdapter recipesListAdapter;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private static final String TAG = RecipesListFragment.class.getSimpleName();

    private static final int SORT_OPTION_LATEST = 1;
    private static final int SORT_OPTION_TITLE_ASCENDING = 2;
    private static final int SORT_OPTION_TITLE_DESCENDING = 3;
    private static final int SORT_OPTION_MY_RECIPES = 4;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
    }

    public static RecipesListFragment getInstance() {return instance;}

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        recipesViewModel =
                new ViewModelProvider(requireActivity()).get(RecipesViewModel.class);

        binding = FragmentRecipesListBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        recipeList = new ArrayList<>();

        recipesListAdapter = new RecipesListAdapter(recipeList);
        recipesListAdapter.setListener(this);

        binding.recyclerViewRecipeList.setLayoutManager(new GridLayoutManager(this.getContext(), 1));
        binding.recyclerViewRecipeList.setAdapter(recipesListAdapter);

        //display recipe list
        displayRecipeList(SORT_OPTION_LATEST); //default sorting option: latest

        binding.btnSortLatest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayRecipeList(SORT_OPTION_LATEST);
            }
        });

        binding.btnSortAscTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayRecipeList(SORT_OPTION_TITLE_ASCENDING);
            }
        });

        binding.btnSortDescTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayRecipeList(SORT_OPTION_TITLE_DESCENDING);
            }
        });

        binding.btnSortMyRecipes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayRecipeList(SORT_OPTION_MY_RECIPES);
            }
        });


        //event handler for add recipe button
        binding.btnAddRecipe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavHostFragment.findNavController(RecipesListFragment.this)
                        .navigate(R.id.action_nav_recipes_to_nav_newRecipe);
            }
        });

        //disable FAB for new rating
        FloatingActionButton fab = getActivity().findViewById(R.id.fab);
        if(fab != null) {
            fab.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onStop() {
        FloatingActionButton fab = getActivity().findViewById(R.id.fab);
        fab.setVisibility(View.VISIBLE);
        super.onStop();
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
    public void onListItemClick(Recipe recipe, int position) {
        recipesViewModel.setSelectedRecipe(recipe);
        recipesViewModel.setSelectedRecipeId(recipeList.get(position).getRecipeId());

        NavHostFragment.findNavController(RecipesListFragment.this).navigate(R.id.action_nav_recipes_to_nav_viewRecipes);
    }

    private void displayRecipeList(int sortOption) {

        Query query;

        switch(sortOption) {
            case SORT_OPTION_TITLE_ASCENDING:
                query = db.collection("recipes")
                        .orderBy("recipeTitle", Query.Direction.ASCENDING);
                readRecipeList(query);
                break;

            case SORT_OPTION_TITLE_DESCENDING:
                query = db.collection("recipes")
                        .orderBy("recipeTitle", Query.Direction.DESCENDING);
                readRecipeList(query);
                break;

            case SORT_OPTION_MY_RECIPES:
                query = db.collection("recipes")
                        .whereEqualTo("userid", user.getUid())
                        .orderBy("timestamp", Query.Direction.DESCENDING);
                readRecipeList(query);
                break;

            default: // by latest
                query = db.collection("recipes")
                        .orderBy("timestamp", Query.Direction.DESCENDING);
                readRecipeList(query);
        }

        //Scroll to top
        binding.recyclerViewRecipeList.smoothScrollToPosition(0);
    }

    private void readRecipeList(Query query){

        query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if(error != null) {
                    Log.w(TAG, "Listen failed.", error);
                    return;
                }

                Log.d(TAG, "excute fetch db, value.size: " + value.size());

                recipeList.clear();
                for(QueryDocumentSnapshot document: value) {
                    if (document != null) {
                        Log.d(TAG, document.getId() + " => " + document.getData());
                        Recipe recipe = document.toObject(Recipe.class);
                        recipeList.add(recipe);
                    }
                }
                recipesListAdapter.setRecipeList(recipeList);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}