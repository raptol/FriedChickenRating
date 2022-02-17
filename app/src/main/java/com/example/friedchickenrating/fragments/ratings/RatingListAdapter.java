package com.example.friedchickenrating.fragments.ratings;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.friedchickenrating.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import com.bumptech.glide.Glide;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RatingListAdapter extends RecyclerView.Adapter {
    private List<Rating> ratingList;
    private List<Place> placeList;
    private ItemClickListener mListener;

    private static final String TAG = RatingListAdapter.class.getSimpleName();

    interface ItemClickListener {
        void onListItemClick(Rating rating, int position);
    }

    public void setListener(ItemClickListener mListener) {
        this.mListener = mListener;
    }

    public RatingListAdapter(List<Rating> ratingList, List<Place> placeList) {
        this.ratingList = ratingList;
        this.placeList = placeList;
    }

    public void setRatingList(List<Rating> ratingList, List<Place> placeList) {
        this.ratingList = ratingList;
        this.placeList = placeList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_rating_list, parent, false);
        RatingViewHolder ratingViewHolder = new RatingViewHolder(view);

        return ratingViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, @SuppressLint("RecyclerView") int position) {

        RatingViewHolder ratingViewHolder = (RatingViewHolder) holder;
        Rating curRating = ratingList.get(position);
        Place place = getPlace(curRating.getPlaceid());

        ratingViewHolder.txtItemTitle.setText(curRating.getTitle());
        ratingViewHolder.txtItemPlace.setText(place.getName());
        ratingViewHolder.ratingBar.setRating(curRating.getStaroverall());

        // download and display images
        Map<String, Object> pictures = curRating.getPictures();
        String filename = String.valueOf(pictures.get("filename"));

        Log.d(TAG, "filename: " + filename);

        if( !filename.isEmpty() && filename != null) {
            long size;
            final FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
            StorageReference storageReference
                    = firebaseStorage.getReferenceFromUrl("gs://friedchickenrating.appspot.com/")
                    .child("images").child(filename);
            storageReference.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        Glide.with(holder.itemView.getContext())
                                .load(task.getResult())
                                .into(((RatingViewHolder) holder).imgItemPhoto);
                    } else {
                        Toast.makeText(holder.itemView.getContext(),
                                "Fail to load image", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }


        ratingViewHolder.imgItemPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onListItemClick(curRating, position);
            }
        });

    }

    @Override
    public int getItemCount() {
        return ratingList.size();
    }

    private Place getPlace(String placeid) {
        Place place = new Place();

        for(int i = 0; i < placeList.size(); i++) {
            if(placeList.get(i).getPlaceid() != null) {
                if (placeList.get(i).getPlaceid().equals(placeid)) {
                    place = placeList.get(i);
                    break;
                }
            }else {
                break;
            }
        }
        return place;
    }

    public static class RatingViewHolder extends RecyclerView.ViewHolder {
        private ImageView imgItemPhoto;
        private TextView txtItemTitle;
        private TextView txtItemPlace;
        private RatingBar ratingBar;

        public RatingViewHolder(@NonNull View itemView) {
            super(itemView);

            imgItemPhoto = itemView.findViewById(R.id.imgItemPhoto);
            txtItemTitle = itemView.findViewById(R.id.txtItemTitle);
            txtItemPlace = itemView.findViewById(R.id.txtItemPlace);
            ratingBar = itemView.findViewById(R.id.ratingBar);
        }
    }
}
