package com.example.friedchickenrating;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.friedchickenrating.fragments.home.HomeFragment;
import com.example.friedchickenrating.fragments.ratings.NewRatingFragment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.example.friedchickenrating.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private static final String TAG = "MainActivity";

    private CardView cardView_map, cardView_favorite, cardView_profile, cardView_roulette;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);
        binding.appBarMain.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Add rating", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();

                NavHostFragment navHostFragment =
                        (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
                NavController navController = navHostFragment.getNavController();
                navController.navigateUp();
                //navController.navigate(R.id.action_nav_home_to_nav_newRating);
                navController.navigate(R.id.nav_newRating);
            }
        });
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_my_favorites, R.id.nav_my_recipes,
                R.id.nav_maps, R.id.nav_roulette, R.id.nav_newRating, R.id.nav_ratings, R.id.nav_recipes)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        View headerView = navigationView.getHeaderView(0);
        ImageView navUserProfile = headerView.findViewById(R.id.imgUserProfile);
        TextView navUserName = headerView.findViewById(R.id.txtUserName);
        TextView navUserEmail = headerView.findViewById(R.id.txtUserEmail);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        //get user from Firestore DB
        DocumentReference usersDbRef = db.collection("users").document(user.getUid());
        usersDbRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if(document.exists()) {
                        User userData = document.toObject(User.class);
                        navUserName.setText(userData.getName());
                        navUserEmail.setText(userData.getEmail());

                        Log.d(TAG, "user name: " + userData.getName() + ", user email: " + userData.getEmail());
                    } else {
                        Log.d(TAG, "No such user data");
                    }
                }
            }
        });

        navUserProfile.setOnClickListener((View view) -> {
            startActivity(new Intent(MainActivity.this, UserProfileActivity.class));
        });

        binding.btnLogout.setOnClickListener((View view) -> {
            auth.signOut();
            startActivity((new Intent(MainActivity.this, LoginActivity.class)));
            finish();
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}