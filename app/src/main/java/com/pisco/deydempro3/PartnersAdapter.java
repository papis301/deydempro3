package com.pisco.deydempro3;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

public class PartnersAdapter
        extends BaseAdapter {

    Activity activity;

    ArrayList<HashMap<String, String>>
            list;

    public PartnersAdapter(

            Activity activity,

            ArrayList<HashMap<String, String>>
                    list
    ){

        this.activity = activity;

        this.list = list;
    }

    @Override
    public int getCount() {

        return list.size();
    }

    @Override
    public Object getItem(int position) {

        return list.get(position);
    }

    @Override
    public long getItemId(int position) {

        return position;
    }

    @Override
    public View getView(

            int position,

            View convertView,

            ViewGroup parent
    ){

        LayoutInflater inflater =
                activity.getLayoutInflater();

        View view =
                inflater.inflate(

                        R.layout.item_partner,

                        null
                );

        TextView txtNom =
                view.findViewById(
                        R.id.txtNom
                );

        TextView txtPhone =
                view.findViewById(
                        R.id.txtPhone
                );

        TextView txtSolde =
                view.findViewById(
                        R.id.txtSolde
                );

        HashMap<String, String> item =
                list.get(position);

        txtNom.setText(
                item.get("nom")
        );

        txtPhone.setText(
                "Téléphone : "
                        + item.get("telephone")
        );

        txtSolde.setText(
                "Solde : "
                        + item.get("solde")
                        + " CFA"
        );

        return view;
    }
}
