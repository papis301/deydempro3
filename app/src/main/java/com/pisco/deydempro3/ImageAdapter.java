package com.pisco.deydempro3;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.VH> {

    Context ctx;
    ArrayList<Uri> images;

    public ImageAdapter(Context ctx, ArrayList<Uri> images) {
        this.ctx = ctx;
        this.images = images;
    }

    @Override
    public VH onCreateViewHolder(ViewGroup p, int v) {
        return new VH(LayoutInflater.from(ctx)
                .inflate(R.layout.item_image, p, false));
    }

    @Override
    public void onBindViewHolder(VH h, int i) {
        h.img.setImageURI(images.get(i));
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView img;
        VH(View v) {
            super(v);
            img = v.findViewById(R.id.imageView);
        }
    }
}

