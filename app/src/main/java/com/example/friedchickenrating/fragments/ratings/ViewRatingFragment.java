package com.example.friedchickenrating.fragments.ratings;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.friedchickenrating.databinding.FragmentNewRatingBinding;
import com.example.friedchickenrating.databinding.FragmentViewRatingBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ViewRatingFragment extends Fragment {

    private RatingViewModel ratingViewModel;
    private FragmentViewRatingBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private ImageView imgViewNewPhoto;
    private Uri filePath;
    private String fileName;

    private static final String TAG = ViewRatingFragment.class.getSimpleName();
    static final int REQUEST_IMAGE_SELECT = 0;
    static final int REQUEST_IMAGE_CAPTURE = 1;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentViewRatingBinding.inflate(inflater, container, false);
        View rootView = binding.getRoot();

        ratingViewModel = new ViewModelProvider(this).get(RatingViewModel.class);

        return rootView;
    }
}
