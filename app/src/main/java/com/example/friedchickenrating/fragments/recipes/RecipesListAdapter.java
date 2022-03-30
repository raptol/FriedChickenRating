package com.example.friedchickenrating.fragments.recipes;

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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;
import java.util.Map;

public class RecipesListAdapter extends RecyclerView.Adapter {
    private List<Recipe> recipeList;
    private ItemClickListener mListener;

    private static final String TAG = RecipesListAdapter.class.getSimpleName();

    interface ItemClickListener {
        void onListItemClick(Recipe recipe, int position);
    }

    public void setListener(ItemClickListener mListener) {
        this.mListener = mListener;
    }

    public RecipesListAdapter(List<Recipe> recipeList) {
        this.recipeList = recipeList;
    }

    public void setRecipeList(List<Recipe> recipeList) {
        this.recipeList = recipeList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_recipes_list, parent,false);
        RecipeViewHolder recipeViewHolder = new RecipeViewHolder(view);
        return recipeViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        RecipeViewHolder recipeViewHolder = (RecipeViewHolder) holder;
        Recipe curRecipe = recipeList.get(position);

        recipeViewHolder.txtRecipeItemTitle.setText(curRecipe.getRecipeTitle());

        Map<String, Object> pictures = curRecipe.getPictures();
        String filename = null;
        if(pictures != null)
            filename = String.valueOf(pictures.get("filename"));

        Log.d(TAG, "filename: " + filename);

        if( filename != null && !filename.isEmpty()) {
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
                                .into(((RecipeViewHolder) holder).imgRecipeItemPhoto);
                        ((RecipeViewHolder) holder).imgRecipeItemPhoto.invalidate();
                    } else {
                        Toast.makeText(holder.itemView.getContext(),
                                "Fail to load image", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        recipeViewHolder.imgRecipeItemPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onListItemClick(curRecipe, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return recipeList.size();
    }

    public static class RecipeViewHolder extends RecyclerView.ViewHolder {
        private ImageView imgRecipeItemPhoto;
        private TextView txtRecipeItemTitle;

        public RecipeViewHolder(@NonNull View itemView) {
            super(itemView);

            imgRecipeItemPhoto = itemView.findViewById(R.id.imgRecipeItemPhoto);
            txtRecipeItemTitle = itemView.findViewById(R.id.txtRecipeItemTitle);
        }
    }
}
