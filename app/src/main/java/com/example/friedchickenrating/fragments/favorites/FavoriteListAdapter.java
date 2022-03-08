package com.example.friedchickenrating.fragments.favorites;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.friedchickenrating.R;
import com.example.friedchickenrating.fragments.ratings.Rating;
import com.example.friedchickenrating.fragments.ratings.RatingPlace;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;
import java.util.Map;

public class FavoriteListAdapter extends RecyclerView.Adapter  {

    private List<Favorite> favoriteList;
    private List<Rating> ratingList;
    private List<RatingPlace> placeList;

    private FavoriteListAdapter.ItemClickListener mListener;

    private static final String TAG = FavoriteListAdapter.class.getSimpleName();

    interface ItemClickListener {
        void onListItemClick(Favorite favorite, Rating rating, int position);
    }

    public void setListener(FavoriteListAdapter.ItemClickListener mListener) {
        this.mListener = mListener;
    }

    public FavoriteListAdapter(List<Favorite> favoriteList, List<Rating> ratingList, List<RatingPlace> placeList) {
        this.favoriteList = favoriteList;
        this.ratingList = ratingList;
        this.placeList = placeList;
    }

    public void setFavoriteList(List<Favorite> favoriteList, List<Rating> ratingList, List<RatingPlace> placeList) {
        this.favoriteList = favoriteList;
        this.ratingList = ratingList;
        this.placeList = placeList;

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_favorite_list, parent, false);
        FavoriteViewHolder favoriteViewHolder = new FavoriteViewHolder(view);

        return favoriteViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        FavoriteViewHolder favoriteViewHolder = (FavoriteViewHolder) holder;
        Favorite curFavorite = favoriteList.get(position);

        Rating curRating = getRating(curFavorite.getRatingid());
        RatingPlace place = getPlace(curRating.getPlaceid());

        favoriteViewHolder.txtItemTitle.setText(curRating.getTitle());
        favoriteViewHolder.txtItemPlace.setText(place.getName());

        // download and display images
        Map<String, Object> pictures = curRating.getPictures();
        String filename = String.valueOf(pictures.get("filename"));

        Log.d(TAG, "filename: " + filename);

        if( !filename.isEmpty() && filename != null) {
            long size;
            final FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
            StorageReference storageReference
                    = firebaseStorage.getReference().child("images").child(filename);
            storageReference.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        Glide.with(holder.itemView.getContext())
                                .load(task.getResult())
                                .into(((FavoriteViewHolder) holder).imgItemPhoto);
                        ((FavoriteViewHolder) holder).imgItemPhoto.invalidate();
                    } else {
                        Toast.makeText(holder.itemView.getContext(),
                                "Fail to load image", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }


        favoriteViewHolder.imgItemPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onListItemClick(curFavorite, curRating, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return favoriteList.size();
    }

    private Rating getRating(String ratingid) {
        Rating rating = new Rating();

        for(int i = 0; i < ratingList.size(); i++) {
            if(ratingList.get(i).getId() != null) {
                if (ratingList.get(i).getId().equals(ratingid)) {
                    rating = ratingList.get(i);
                    break;
                }
            }else {
                break;
            }
        }
        return rating;
    }

    private RatingPlace getPlace(String placeid) {
        RatingPlace place = new RatingPlace();

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

    public static class FavoriteViewHolder extends RecyclerView.ViewHolder {
        private ImageView imgItemPhoto;
        private TextView txtItemTitle;
        private TextView txtItemPlace;

        public FavoriteViewHolder(@NonNull View itemView) {
            super(itemView);

            imgItemPhoto = itemView.findViewById(R.id.imgItemPhoto);
            txtItemTitle = itemView.findViewById(R.id.txtItemTitle);
            txtItemPlace = itemView.findViewById(R.id.txtItemPlace);
        }
    }
}
