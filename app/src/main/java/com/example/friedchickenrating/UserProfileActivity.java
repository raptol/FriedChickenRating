package com.example.friedchickenrating;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.friedchickenrating.databinding.ActivityUserProfileBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

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
                        binding.editTxtBirthYear.setText(userData.getBirthYear());

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

            //update user's info to Firestore DB
            User updUserData = new User(userData.getUid(), userData.getName(), userData.getEmail(),
                                        binding.editTxtBirthYear.getText().toString().trim(),
                                        binding.ratingBarFlavor.getRating(),
                                        binding.ratingBarCrunch.getRating(),
                                        binding.ratingBarSpiciness.getRating(),
                                        binding.ratingBarPortion.getRating(),
                                        binding.ratingBarPrice.getRating()
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