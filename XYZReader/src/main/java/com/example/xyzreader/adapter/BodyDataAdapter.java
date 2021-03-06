package com.example.xyzreader.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.xyzreader.R;

import java.util.List;

public class BodyDataAdapter extends RecyclerView.Adapter<BodyDataAdapter.BodyViewHolder> {
    List<String> bodyDataList;
    Context context;

    public BodyDataAdapter(Context context) {
        this.context = context;
    }

    public void swapBodyDataList(List<String> bodyDataList) {
        this.bodyDataList = bodyDataList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BodyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.simple_textview, parent, false);
        return new BodyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BodyViewHolder holder, int position) {
        holder.myTextView.setText(bodyDataList.get(position));
        if (position == 0) {
            holder.myTextView.setBackground(ContextCompat.getDrawable(context, R.drawable.top_corners_background));
        } else if (position == getItemCount() - 1) {
            holder.myTextView.setBackground(ContextCompat.getDrawable(context, R.drawable.bottom_corners_background));
        } else {
            holder.myTextView.setBackgroundColor(ContextCompat.getColor(context, R.color.grey_300));
        }

    }

    @Override
    public int getItemCount() {
        return bodyDataList == null ? 0 : bodyDataList.size();
    }


    public class BodyViewHolder extends RecyclerView.ViewHolder {
        TextView myTextView;

        BodyViewHolder(View itemView) {
            super(itemView);
            myTextView = itemView.findViewById(R.id.simpleTextView);
        }

    }

}
