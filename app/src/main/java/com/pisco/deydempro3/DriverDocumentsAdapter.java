package com.pisco.deydempro3;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class DriverDocumentsAdapter
        extends RecyclerView.Adapter<DriverDocumentsAdapter.ViewHolder>{

    private final List<DriverDocument> list;

    private final OnDocumentClickListener listener;

    public DriverDocumentsAdapter(
            List<DriverDocument> list,
            OnDocumentClickListener listener){

        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType){

        View view =
                LayoutInflater
                        .from(parent.getContext())
                        .inflate(
                                R.layout.item_document,
                                parent,
                                false
                        );

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position){

        DriverDocument doc =
                list.get(position);

        String type = doc.getType();

        boolean noVerso =
                type.equals("Photo de profil")
                        || type.equals("Photo véhicule avant")
                        || type.equals("Photo véhicule arrière")
                        || type.equals("Photo véhicule gauche")
                        || type.equals("Photo véhicule droite");

        if(noVerso){

            holder.btnVerso.setVisibility(View.GONE);
            holder.imgVerso.setVisibility(View.GONE);

        }else{

            holder.btnVerso.setVisibility(View.VISIBLE);
            holder.imgVerso.setVisibility(View.VISIBLE);
        }

        holder.txtType.setText(
                doc.getType()
        );

        /*
         * STATUT
         */
        if(doc.isApproved()){

            holder.txtStatus.setText(
                    "🟢 Document validé"
            );

        }else if(doc.isRejected()){

            holder.txtStatus.setText(
                    "🔴 Document rejeté"
            );

        }else{

            holder.txtStatus.setText(
                    "🟡 En attente de validation"
            );
        }

        /*
         * MOTIF
         */
        if(
                doc.getReason() == null
                        || doc.getReason().trim().isEmpty()
        ){

            holder.txtReason.setVisibility(
                    View.GONE
            );

        }else{

            holder.txtReason.setVisibility(
                    View.VISIBLE
            );

            holder.txtReason.setText(
                    doc.getReason()
            );
        }

        /*
         * RECTO
         */

        if(doc.getRectoBitmap() != null){

            holder.imgRecto.setImageBitmap(
                    doc.getRectoBitmap()
            );

        }else if(
                doc.getRectoUrl() != null
                        &&
                        !doc.getRectoUrl().isEmpty()
        ){

            Glide.with(
                            holder.itemView.getContext()
                    )
                    .load(
                            doc.getRectoUrl()
                    )
                    .into(
                            holder.imgRecto
                    );

        }else{

            holder.imgRecto.setImageResource(
                    android.R.color.darker_gray
            );
        }

        /*
         * VERSO
         */

        if(doc.getVersoBitmap() != null){

            holder.imgVerso.setImageBitmap(
                    doc.getVersoBitmap()
            );

        }else if(
                doc.getVersoUrl() != null
                        &&
                        !doc.getVersoUrl().isEmpty()
        ){

            Glide.with(
                            holder.itemView.getContext()
                    )
                    .load(
                            doc.getVersoUrl()
                    )
                    .into(
                            holder.imgVerso
                    );

        }else{

            holder.imgVerso.setImageResource(
                    android.R.color.darker_gray
            );
        }

        /*
         * BOUTONS
         */

        boolean uploaded =
                doc.getRectoUrl() != null
                        &&
                        !doc.getRectoUrl()
                                .trim()
                                .isEmpty();

        if(uploaded){

            holder.btnUpload.setVisibility(
                    View.GONE
            );

            holder.btnUpdate.setVisibility(
                    View.VISIBLE
            );

        }else{

            holder.btnUpload.setVisibility(
                    View.VISIBLE
            );

            holder.btnUpdate.setVisibility(
                    View.GONE
            );
        }

        /*
         * RECTO
         */

        holder.btnRecto.setOnClickListener(v -> {

            listener.onTakeRecto(doc);

        });

        /*
         * VERSO
         */

        holder.btnVerso.setOnClickListener(v -> {

            listener.onTakeVerso(doc);

        });

        /*
         * ENVOYER
         */

        holder.btnUpload.setOnClickListener(v -> {

            listener.onUploadDocument(doc);

        });

        /*
         * MISE A JOUR
         */

        holder.btnUpdate.setOnClickListener(v -> {

            listener.onUpdateDocument(doc);

        });
    }

    @Override
    public int getItemCount(){

        return list.size();
    }

    static class ViewHolder
            extends RecyclerView.ViewHolder{

        TextView txtType;
        TextView txtStatus;
        TextView txtReason;

        ImageView imgRecto;
        ImageView imgVerso;

        Button btnRecto;
        Button btnVerso;

        Button btnUpload;
        Button btnUpdate;

        public ViewHolder(
                @NonNull View itemView){

            super(itemView);

            txtType =
                    itemView.findViewById(
                            R.id.txtType
                    );

            txtStatus =
                    itemView.findViewById(
                            R.id.txtStatus
                    );

            txtReason =
                    itemView.findViewById(
                            R.id.txtReason
                    );

            imgRecto =
                    itemView.findViewById(
                            R.id.imgRecto
                    );

            imgVerso =
                    itemView.findViewById(
                            R.id.imgVerso
                    );

            btnRecto =
                    itemView.findViewById(
                            R.id.btnRecto
                    );

            btnVerso =
                    itemView.findViewById(
                            R.id.btnVerso
                    );

            btnUpload =
                    itemView.findViewById(
                            R.id.btnUpload
                    );

            btnUpdate =
                    itemView.findViewById(
                            R.id.btnUpdate
                    );
        }
    }

    public interface OnDocumentClickListener{

        void onTakeRecto(
                DriverDocument document
        );

        void onTakeVerso(
                DriverDocument document
        );

        void onUploadDocument(
                DriverDocument document
        );

        void onUpdateDocument(
                DriverDocument document
        );
    }
}