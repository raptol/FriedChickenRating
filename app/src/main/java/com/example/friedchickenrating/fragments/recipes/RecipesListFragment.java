package com.example.friedchickenrating.fragments.recipes;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
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

    private boolean isBackPressed = false;

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

        //display rating list
        int previousFilter = recipesViewModel.getFilter().getValue();
        if( previousFilter != 0)
            isBackPressed = true;

        if(!isBackPressed) {
            displayRecipeList(SORT_OPTION_LATEST, 0); //default sorting option: latest
        }
        else {
            displayRecipeList(previousFilter, recipesViewModel.getSelectedPosition().getValue()); //set previous filter
        }

        binding.btnSortLatest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayRecipeList(SORT_OPTION_LATEST, 0);
            }
        });

        binding.btnSortAscTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayRecipeList(SORT_OPTION_TITLE_ASCENDING, 0);
            }
        });

        binding.btnSortDescTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayRecipeList(SORT_OPTION_TITLE_DESCENDING, 0);
            }
        });

        binding.btnSortMyRecipes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayRecipeList(SORT_OPTION_MY_RECIPES, 0);
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

    @Override
    public void onListItemClick(Recipe recipe, int position) {
        recipesViewModel.setSelectedRecipe(recipe);
        recipesViewModel.setSelectedRecipeId(recipeList.get(position).getRecipeId());
        recipesViewModel.setSelectedPosition(position);

        NavHostFragment.findNavController(RecipesListFragment.this).navigate(R.id.action_nav_recipes_to_nav_viewRecipes);
    }

    private void displayRecipeList(int sortOption, int scrollToPosition) {

        recipesViewModel.setFilter(sortOption);

        //set background color of the filter buttons
        switchFilterBackgroundColor(sortOption);

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

        //Scroll to position
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                binding.recyclerViewRecipeList.scrollToPosition(scrollToPosition);
            }
        }, 500);

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

    private void switchFilterBackgroundColor(int filter) {
        float opaque = 1.0f; //non-transparent

        //recover the button color of the filters
        binding.btnSortLatest.setAlpha(opaque);
        binding.btnSortAscTitle.setAlpha(opaque);
        binding.btnSortDescTitle.setAlpha(opaque);
        binding.btnSortMyRecipes.setAlpha(opaque);

        //set background color of the filter that is selected
        opaque = 0.5f; //transparent
        switch(filter) {
            case SORT_OPTION_TITLE_ASCENDING:
                binding.btnSortAscTitle.setAlpha(opaque);
                break;
            case SORT_OPTION_TITLE_DESCENDING:
                binding.btnSortDescTitle.setAlpha(opaque);
                break;
            case SORT_OPTION_MY_RECIPES:
                binding.btnSortMyRecipes.setAlpha(opaque);
                break;
            default:
                binding.btnSortLatest.setAlpha(opaque); // LATEST
                break;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}