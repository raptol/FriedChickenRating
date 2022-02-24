package com.example.friedchickenrating;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.friedchickenrating.databinding.ActivityLoginBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private static final int PASSWORD_MIN_CHARS = 6;
    private static final String TAG = "LoginActivity";

    private User userData;
    Boolean isFirstLogin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        binding.btnLogin.setOnClickListener((View view) -> {
            if(checkEmailFormat() == false)
                return;

            if(checkPasswordFormat() == false)
                return;

            binding.inputLayoutEmail.setErrorEnabled(false);
            binding.inputLayoutPassword.setErrorEnabled(false);

            auth.signInWithEmailAndPassword(binding.editTxtEmail.getText().toString().trim(),
                                            binding.editTxtPassword.getText().toString().trim())
                    .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(Task<AuthResult> task) {
                            if(task.isSuccessful()) {
                                FirebaseUser user = auth.getCurrentUser();
                                if(user.isEmailVerified() == false) {
                                    Toast.makeText(getApplicationContext(), "Please verify your email.",
                                            Toast.LENGTH_SHORT).show();
                                }

                                //get user from Firestore DB
                                DocumentReference usersDbRef = db.collection("users").document(user.getUid());
                                usersDbRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                        if(task.isSuccessful()) {
                                            DocumentSnapshot document = task.getResult();
                                            if(document.exists()) {
                                                userData = document.toObject(User.class);

                                                Log.d(TAG, "getLastLogin(): " + userData.getLastlogin());
                                                if(userData.getLastlogin() == null) {
                                                    isFirstLogin = true;
                                                }

                                                // update last login time of user to Firestore DB
                                                db.collection("users").document(user.getUid())
                                                        .update("lastlogin", Timestamp.now())
                                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                            @Override
                                                            public void onSuccess(Void aVoid) {
                                                                Log.d(TAG, "User was successfully saved!");
                                                            }
                                                        })
                                                        .addOnFailureListener(new OnFailureListener() {
                                                            @Override
                                                            public void onFailure(@NonNull Exception e) {
                                                                Log.w(TAG, "Error saving data", e);
                                                            }
                                                        });
                                                Log.d(TAG, "user name: " + userData.getName() + ", user email: " + userData.getEmail());

                                                if(isFirstLogin) {
                                                    Intent intent = new Intent(LoginActivity.this, UserProfileActivity.class);
                                                    intent.putExtra("isFirstLogin", true);
                                                    startActivity(intent);
                                                    finish();
                                                } else {
                                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                                    finish();
                                                }

                                            } else {
                                                Log.d(TAG, "No such user data");
                                            }
                                        }
                                    }
                                });

                            } else {
                                Toast.makeText(LoginActivity.this, getString(R.string.login_failed),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        });


        binding.btnSignup.setOnClickListener((View view) -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });
    }

    private boolean checkEmailFormat() {
        String email = binding.editTxtEmail.getText().toString().trim();
        if (email.isEmpty() || isEmailValid(email) == false) {
            binding.inputLayoutEmail.setErrorEnabled(true);
            binding.inputLayoutEmail.setError(getString(R.string.error_invalid_email));

            return false;
        }

        return true;
    }

    private boolean checkPasswordFormat() {

        String password = binding.editTxtPassword.getText().toString().trim();
        if (password.isEmpty() || isPasswordValid(password) == false) {
            binding.inputLayoutPassword.setErrorEnabled(true);
            binding.inputLayoutPassword.setError(getString(R.string.error_invalid_password));

            return false;
        }

        return true;
    }

    private static boolean isEmailValid(String email) {
        return !TextUtils.isEmpty(email)
                && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private static boolean isPasswordValid(String password) {
        return (password.length() >= PASSWORD_MIN_CHARS);
    }
}