package com.example.friedchickenrating;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.friedchickenrating.databinding.ActivitySignupBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignupActivity extends AppCompatActivity {

    private ActivitySignupBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private static final int PASSWORD_MIN_CHARS = 6;
    private static final String TAG = "Signup";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        binding.btnSignup.setOnClickListener((View view) -> {
            if(checkEmailFormat() == false)
                return;

            if(checkPasswordFormat() == false)
                return;

            binding.inputLayoutName.setErrorEnabled(false);
            binding.inputLayoutEmail.setErrorEnabled(false);
            binding.inputLayoutPassword.setErrorEnabled(false);

            auth.createUserWithEmailAndPassword(binding.editTxtEmail.getText().toString().trim(),
                                                binding.editTxtPassword.getText().toString().trim())
                    .addOnCompleteListener(SignupActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(Task<AuthResult> task) {
                            if(task.isSuccessful()) {

                                FirebaseUser user = auth.getCurrentUser();
                                if(user == null) {
                                    Log.d(TAG, "There is no current user.");
                                } else {
                                    auth.getCurrentUser().sendEmailVerification();
                                    Toast.makeText(getApplicationContext(), "Sent verification email", Toast.LENGTH_SHORT).show();

                                    //add user to Firestore DB
                                    User signupUser = new User(user.getUid(),
                                                                binding.editTxtName.getText().toString().trim(),
                                                                binding.editTxtEmail.getText().toString().trim(),
                                                                null, 0.0, 0.0, null, null,
                                                                0, 0, 0, 0, 0,
                                                                null, Timestamp.now(), null);
                                    db.collection("users").document(user.getUid())
                                            .set(signupUser)
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

                                    Toast.makeText(getApplicationContext(), "Signup success.", Toast.LENGTH_SHORT).show();
                                    startActivity((new Intent(SignupActivity.this, LoginActivity.class)));
                                }

                                finish();
                            } else {
                                Log.d(TAG, "Signup failed." + task.getException());
                                Toast.makeText(getApplicationContext(), "Signup failed. Or, the email is already in use.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        });


        binding.btnLogin.setOnClickListener((View view) -> {
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
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