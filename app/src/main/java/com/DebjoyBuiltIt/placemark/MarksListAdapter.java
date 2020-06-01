package com.DebjoyBuiltIt.placemark;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.vanniktech.emoji.EmojiTextView;

import io.realm.RealmResults;

public class MarksListAdapter extends RecyclerView.Adapter<MarksListAdapter.MarksViewHolder> {

    private MapsActivity mapsActivity;
    private RealmResults<MarkerModel> markerModels;
    private boolean DARK_THEME;
    MarksListAdapter(MapsActivity mapsActivity, RealmResults<MarkerModel> markerModels,boolean DARK_THEME){
        this.mapsActivity=mapsActivity;
        this.markerModels=markerModels;
        this.DARK_THEME=DARK_THEME;
    }

    @NonNull
    @Override
    public MarksViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_marks_list,parent, false);
        MarksViewHolder holder= new MarksViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull MarksViewHolder holder, int position) {
        final MarkerModel markerModel=markerModels.get(position);
        holder.markerTitle.setText(markerModel.getTitle());
        holder.markerEmoji.setText(markerModel.getEmoji());

        Drawable ImageViewDrawable = holder.markerColor.getBackground();
        String tempColor=markerModel.getColor();
        if(tempColor.equals("#222f3e") && DARK_THEME)
            tempColor="#f0f6ff";
        DrawableCompat.setTint(ImageViewDrawable, Color.parseColor(tempColor));
        holder.markerColor.setBackground(ImageViewDrawable);

        holder.markerEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapsActivity.editLocation(markerModel.getId());
            }
        });

        holder.markerDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapsActivity.deleteLocation(markerModel.getId());
            }
        });

        holder.markerLocate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapsActivity.focusMarker(markerModel.getId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return markerModels.size();
    }

    public class MarksViewHolder extends RecyclerView.ViewHolder{
        private ImageView markerColor;
        private EmojiTextView markerEmoji;
        private TextView markerTitle;
        private LinearLayout markerEdit;
        private LinearLayout markerDelete;
        private LinearLayout markerLocate;

        public MarksViewHolder(@NonNull View itemView) {
            super(itemView);
            markerColor=itemView.findViewById(R.id.marker_color);
            markerEmoji=itemView.findViewById(R.id.marker_emoji);
            markerTitle=itemView.findViewById(R.id.marker_title);
            markerEdit=itemView.findViewById(R.id.marker_edit);
            markerLocate=itemView.findViewById(R.id.marker_locate);
            markerDelete=itemView.findViewById(R.id.marker_delete);
        }
    }
}
