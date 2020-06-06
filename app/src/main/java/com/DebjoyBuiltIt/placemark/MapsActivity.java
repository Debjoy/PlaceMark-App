package com.DebjoyBuiltIt.placemark;

import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.basgeekball.awesomevalidation.AwesomeValidation;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;

import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.leinardi.android.speeddial.SpeedDialActionItem;
import com.leinardi.android.speeddial.SpeedDialView;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState;
import com.vanniktech.emoji.EmojiEditText;
import com.vanniktech.emoji.EmojiManager;
import com.vanniktech.emoji.EmojiPopup;
import com.vanniktech.emoji.EmojiTextView;
import com.vanniktech.emoji.twitter.TwitterEmojiProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;
import petrov.kristiyan.colorpicker.ColorPicker;

import static com.basgeekball.awesomevalidation.ValidationStyle.BASIC;
import static com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState.COLLAPSED;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationClickListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnMapLoadedCallback {

    private GoogleMap mMap;
    private LinearLayout mMapCover;
    private UiSettings mUiSettings;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private SpeedDialView speedDialView;
    private SpeedDialActionItem deleteMarkerFab;
    private SpeedDialActionItem editMarkerFab;
    private SpeedDialActionItem currentMarkerFab;
    private SpeedDialActionItem saveMarkerFab;
    private Button myLocationButton;

    private ColorStateList mColorPrimary;
    private ColorStateList mColorAccent;
    private ColorStateList mColorText;
    private ColorStateList mColorTextTitle;
    private ColorStateList mColorGrey;
    private ColorStateList mColorSuccess;
    private ColorStateList mColorBackground;
    private ColorStateList mColorMarker;
    private ColorStateList mColorWhite;
    private ColorStateList mColorDanger;

    private boolean DARK_THEME=false;


    private ImageView notifyItem;

    private BiMap<Long, Marker> markerList;
    private long markerId;

    private Marker selectedMarker;
    private String defaultSelectedColor = "";
    private SlidingUpPanelLayout slidingPaneLayout;
    private LinearLayout slidingLayoutContent;

    private RecyclerView markListRecyclerView;
    private LinearLayout noMarkerSaved;

    private Realm realm;

    private Context mContext;

    private ArrayList<String> colorHexList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences sharedpreferences = getSharedPreferences("PlaceMarkApp", Context.MODE_PRIVATE);
        DARK_THEME=sharedpreferences.getBoolean("DarkTheme",false);
        //DARK_THEME=false;//Toggle this to change theme
        if(DARK_THEME)
            setTheme(R.style.DarkTheme);
        else {
            setTheme(R.style.LightTheme);
        }
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
            getWindow().setNavigationBarColor(mColorAccent.getDefaultColor());
        }
        //getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_maps);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setLogo(R.drawable.ic_icon_action);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
        getSupportActionBar().setTitle(" PlaceMark");

        setColorsToColorStateList();

        realm = Realm.getDefaultInstance();


        EmojiManager.install(new TwitterEmojiProvider());

        speedDialView = findViewById(R.id.speedDial);
        setUpFabItems();

        markerList = HashBiMap.create();//Bi directional Hashmap
        markerId = -1;

        markListRecyclerView = findViewById(R.id.marker_list_recycler);
        noMarkerSaved = findViewById(R.id.no_markers_message);
        slidingPaneLayout = findViewById(R.id.sliding_layout);
        slidingLayoutContent = findViewById(R.id.sliding_layout_content);
        notifyItem = findViewById(R.id.new_item_notify);
        myLocationButton = findViewById(R.id.my_location_button);

        float topRadius = 60f;
        GradientDrawable shape = new GradientDrawable();
        float radii[] = {topRadius, topRadius, topRadius, topRadius,
                0f, 0f, 0f, 0f};
        shape.setCornerRadii(radii);
        shape.setColor(mColorPrimary);
        slidingLayoutContent.setBackground(shape);


        speedDialView.setOnActionSelectedListener(new SpeedDialView.OnActionSelectedListener() {
            @Override
            public boolean onActionSelected(SpeedDialActionItem actionItem) {
                switch (actionItem.getId()) {
                    case R.id.fab_selected_location:
                        saveLocation(false);
                        break;
                    case R.id.fab_delete_location:
                        deleteLocation(markerId);
                        break;
                    case R.id.fab_edit_location:
                        editLocation(markerId);
                        break;
                    case R.id.fab_current_location:
                        saveCurrentLocation();
                        break;
                }
                return false;
            }
        });

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mMapCover=findViewById(R.id.map_cover);


        slidingPaneLayout.addPanelSlideListener(new SlidingUpPanelLayout.SimplePanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {

                float topRadius = 60f * (1 - slideOffset);
                GradientDrawable shape = new GradientDrawable();
                float radii[] = {topRadius, topRadius, topRadius, topRadius,
                        0f, 0f, 0f, 0f};
                shape.setCornerRadii(radii);
                shape.setColor(mColorPrimary);
                slidingLayoutContent.setBackground(shape);
            }

            @Override
            public void onPanelStateChanged(View panel, PanelState previousState, PanelState newState) {
                if (newState == COLLAPSED) {
                    notifyItem.setVisibility(View.INVISIBLE);
                }
            }
        });

        myLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isLocationEnabled(mContext)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext, R.style.CustomDialog);
                    LayoutInflater inflater = LayoutInflater.from(mContext);
                    final View layout=inflater.inflate(R.layout.alert_template,null);
                    builder.setView(layout);
                    final AlertDialog alertD=builder.show();
                    ((TextView)layout.findViewById(R.id.alert_text)).setText("You have to enable your location services first.");
                    TextView alertTitle=layout.findViewById(R.id.alert_title);
                    alertTitle.setText("Enable Location");
                    Button confirmButton=layout.findViewById(R.id.alert_confirm);
                    confirmButton.setText("OK");
                    confirmButton.setTextColor(mColorTextTitle);
                    View titleLogo=layout.findViewById(R.id.alert_title_logo);
                    titleLogo.setBackground(ResourcesCompat.getDrawable(getResources(),R.drawable.ic_info_black_24dp,getTheme()));

                    Drawable ImageViewDrawable = titleLogo.getBackground();
                    DrawableCompat.setTint(ImageViewDrawable, mColorAccent.getDefaultColor() );
                    titleLogo.setBackground(ImageViewDrawable);

                    layout.findViewById(R.id.alert_close).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            alertD.dismiss();
                        }
                    });

                    layout.findViewById(R.id.alert_confirm).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            alertD.dismiss();
                        }
                    });

                }else{
                    LatLng myLocation=getMyLocation();
                    if(myLocation != null) {
                        if(mMap.getCameraPosition().zoom<=15.0)
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation,17),1000,null);
                        else
                            mMap.animateCamera(CameraUpdateFactory.newLatLng(myLocation),400,null);
                    }
                }
            }
        });

    }

    public void saveLocation(boolean current){

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext, R.style.CustomDialog);
        LayoutInflater inflater = LayoutInflater.from(mContext);
        final View layout=inflater.inflate(R.layout.alert_save_location,null);
        builder.setView(layout);



        ((Button)layout.findViewById(R.id.alert_save_marker_button)).setText("Mark");

        if(current){
            TextView alertTitle=layout.findViewById(R.id.alert_save_title);
            alertTitle.setText("Mark Current Location");
            View titleLogo=layout.findViewById(R.id.alert_save_title_logo);
            titleLogo.setBackground(ResourcesCompat.getDrawable(getResources(),R.drawable.ic_my_location_black_24dp,getTheme()));

            Drawable ImageViewDrawable = titleLogo.getBackground();
            DrawableCompat.setTint(ImageViewDrawable,mColorAccent.getDefaultColor() );
            titleLogo.setBackground(ImageViewDrawable);
        }
        else{
            TextView alertTitle=layout.findViewById(R.id.alert_save_title);
            alertTitle.setText("Mark Selected Location");
            View titleLogo=layout.findViewById(R.id.alert_save_title_logo);
            titleLogo.setBackground(ResourcesCompat.getDrawable(getResources(),R.drawable.ic_bookmark_black_24dp,getTheme()));

            Drawable ImageViewDrawable = titleLogo.getBackground();
            DrawableCompat.setTint(ImageViewDrawable, mColorAccent.getDefaultColor() );
            titleLogo.setBackground(ImageViewDrawable);
        }

        final AwesomeValidation mAwesomeValidation = new AwesomeValidation(BASIC);
        final EditText mMarkerTitle=layout.findViewById(R.id.alert_save_edit_text);
        mAwesomeValidation.addValidation(mMarkerTitle,"^(?!\\s*$)[\\s\\S]{1,50}$","Title must have less than 50 characters");
        mAwesomeValidation.addValidation(mMarkerTitle,"^(?!\\s*$)[\\s\\S]{1,}$","Title can't be empty");

        final AlertDialog alertD=builder.show();

        layout.findViewById(R.id.alert_save_marker_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mAwesomeValidation.validate()){
                    String emojiText=((EmojiTextView)layout.findViewById(R.id.alert_save_emoji_preview)).getText().toString();
                    //Add the marker
                    String tempColor=defaultSelectedColor;
                    if(tempColor.equals("#f0f6ff"))
                        tempColor="#222f3e";
                    addMarker(mMarkerTitle.getText().toString(),emojiText,tempColor,selectedMarker.getPosition().latitude,selectedMarker.getPosition().longitude);

                    alertD.dismiss();

                }
            }
        });

        ((Switch)layout.findViewById(R.id.alert_save_emoji_enable)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked)
                    ((EmojiTextView)layout.findViewById(R.id.alert_save_emoji_preview))
                            .setText(((EmojiTextView)layout.findViewById(R.id.alert_save_emoji_button)).getText().toString());
                else
                    ((EmojiTextView)layout.findViewById(R.id.alert_save_emoji_preview)).setText(" ");
            }
        });


        ((EmojiTextView)layout.findViewById(R.id.alert_save_emoji_button)).setText("🏡");
        if(((Switch)layout.findViewById(R.id.alert_save_emoji_enable)).isChecked())
        ((EmojiTextView)layout.findViewById(R.id.alert_save_emoji_preview)).setText("🏡");
        else
        ((EmojiTextView)layout.findViewById(R.id.alert_save_emoji_preview)).setText(" ");
        final EmojiEditText emojiEditText=(EmojiEditText)layout.findViewById(R.id.alert_emojiEditText);
        layout.findViewById(R.id.alert_save_emoji_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                emojiEditText.setVisibility(View.VISIBLE);

                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                LayoutInflater inflater = LayoutInflater.from(mContext);
                final View pickerLayout=inflater.inflate(R.layout.alert_emoji_picker,null);
                builder.setView(pickerLayout);
                final AlertDialog picker=builder.show();

                emojiEditText.setText("");
                EmojiPopup.Builder emojiBuilder=EmojiPopup.Builder.fromRootView(pickerLayout);
                //Open up emoji keyboard
//                emojiBuilder.setBackgroundColor(mColorPrimary.getDefaultColor());
               emojiBuilder.setDividerColor(mColorGrey.getDefaultColor());
//                emojiBuilder.setIconColor(mColorGrey.getDefaultColor());

                final EmojiPopup emojiPopup=emojiBuilder.build(emojiEditText);
                emojiPopup.toggle();
                //Hide Default Keyboard
                InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                emojiPopup.toggle();
                //get emoji from edit text to preview
                emojiEditText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override
                    public void afterTextChanged(Editable s) {
                        emojiPopup.dismiss();
                        picker.dismiss();
                        emojiPopup.dismiss();
                    }
                });

                picker.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if(!emojiEditText.getText().toString().equals("")) {
                            ((EmojiTextView) layout.findViewById(R.id.alert_save_emoji_button)).setText(emojiEditText.getText().toString());
                            if(((Switch)layout.findViewById(R.id.alert_save_emoji_enable)).isChecked())
                                ((EmojiTextView) layout.findViewById(R.id.alert_save_emoji_preview)).setText(emojiEditText.getText().toString());
                            else
                                ((EmojiTextView) layout.findViewById(R.id.alert_save_emoji_preview)).setText(" ");
                        }
                        emojiEditText.setVisibility(View.GONE);
                    }
                });
            }
        });

        fillColorList();//Fill colors
        defaultSelectedColor=colorHexList.get(0);
        layout.findViewById(R.id.alert_save_color).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ColorPicker colorPicker = new ColorPicker(MapsActivity.this);
                colorPicker.setOnFastChooseColorListener(new ColorPicker.OnFastChooseColorListener() {
                    @Override
                    public void setOnFastChooseColorListener(int position, int color) {
                        // put code
                        defaultSelectedColor=colorHexList.get(position);
                        Drawable buttonDrawable = (layout.findViewById(R.id.alert_save_color)).getBackground();
                        buttonDrawable = DrawableCompat.wrap(buttonDrawable);
                        (layout.findViewById(R.id.alert_save_color)).setBackground(buttonDrawable);
                        DrawableCompat.setTint(buttonDrawable, color);
                        //the color is a direct color int and not a color resource
                        Drawable ImageViewDrawable = (layout.findViewById(R.id.alert_save_marker_body)).getBackground();
                        DrawableCompat.setTint(ImageViewDrawable, color);
                        (layout.findViewById(R.id.alert_save_marker_body)).setBackground(ImageViewDrawable);
                    }

                    @Override
                    public void onCancel(){
                        // put code
                    }
                })
                        .setColors(colorHexList)
                        .setDefaultColorButton(Color.parseColor(defaultSelectedColor))
                        .setColumns(5)
                        .setColorButtonTickColor(mColorWhite.getDefaultColor())
                        .setRoundColorButton(true)
                        .setTitle("Choose a color for marker")
                        .show();
                colorPicker.getDialogBaseLayout().setBackgroundColor(mColorPrimary.getDefaultColor());
            }
        });
        //builder.setTitle("hello");
        alertD.findViewById(R.id.alert_save_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertD.dismiss();
            }
        });
    }
    public void saveCurrentLocation(){
        if(!isLocationEnabled(mContext)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext, R.style.CustomDialog);
            LayoutInflater inflater = LayoutInflater.from(mContext);
            final View layout=inflater.inflate(R.layout.alert_template,null);
            builder.setView(layout);
            final AlertDialog alertD=builder.show();
            ((TextView)layout.findViewById(R.id.alert_text)).setText("You have to enable your location services first.");
            TextView alertTitle=layout.findViewById(R.id.alert_title);
            alertTitle.setText("Enable Location");
            Button confirmButton=layout.findViewById(R.id.alert_confirm);
            confirmButton.setText("OK");
            confirmButton.setTextColor(mColorAccent);
            View titleLogo=layout.findViewById(R.id.alert_title_logo);
            titleLogo.setBackground(ResourcesCompat.getDrawable(getResources(),R.drawable.ic_info_black_24dp,getTheme()));

            Drawable ImageViewDrawable = titleLogo.getBackground();
            DrawableCompat.setTint(ImageViewDrawable, mColorAccent.getDefaultColor() );
            titleLogo.setBackground(ImageViewDrawable);

            layout.findViewById(R.id.alert_close).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    alertD.dismiss();
                }
            });

            layout.findViewById(R.id.alert_confirm).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    alertD.dismiss();
                }
            });

        }else{
            LatLng currentLocation=getMyLocation();
            if(currentLocation!=null){
                View marker = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.custom_marker_layout, null);
                ((EmojiTextView)marker.findViewById(R.id.emojicon_icon)).setText("");
                if(selectedMarker!=null)
                    selectedMarker.remove();
                selectedMarker=mMap.addMarker(new MarkerOptions().position(currentLocation)
                        .title("Untitled")
                        .icon(BitmapDescriptorFactory.fromBitmap(createDrawableFromView(mContext, marker)))
                );
                selectedMarker.setDraggable(true);
                saveLocation(true);
            }
        }
    }
    public void addMarker(String markerTitle,String markerEmoji,String markerColor,double markerLatitude, double markerLongitude){

        long tempId=System.currentTimeMillis() / 1000;


        View marker = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.custom_marker_layout, null);
        ((EmojiTextView)marker.findViewById(R.id.emojicon_icon)).setText(markerEmoji);

        Drawable ImageViewDrawable = (marker.findViewById(R.id.custom_marker_background)).getBackground();
        DrawableCompat.setTint(ImageViewDrawable, Color.parseColor(markerColor));
        (marker.findViewById(R.id.custom_marker_background)).setBackground(ImageViewDrawable);

        //Saving in DATABASE
        realm.beginTransaction();
        try {
            MarkerModel markerModel = realm.createObject(MarkerModel.class, tempId);
            markerModel.setTitle(markerTitle);
            markerModel.setColor(markerColor);
            markerModel.setEmoji(markerEmoji);
            markerModel.setLatitude(markerLatitude);
            markerModel.setLongitude(markerLongitude);
            realm.commitTransaction();
            Toast.makeText(mContext, "Success", Toast.LENGTH_SHORT).show();
            notifyChange();
        }catch (Exception ex){
            realm.cancelTransaction();
            Toast.makeText(mContext, "Failure : " + ex.getMessage(), Toast.LENGTH_SHORT).show();
        }

        loadMarkers();
    }

    public void deleteLocation(final long markerId){
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext, R.style.CustomDialog);
        LayoutInflater inflater = LayoutInflater.from(mContext);
        final View layout=inflater.inflate(R.layout.alert_template,null);
        builder.setView(layout);
        final AlertDialog alertD=builder.show();
        ((TextView)layout.findViewById(R.id.alert_text)).setText("Are you sure you want to remove the selected marker");
        TextView alertTitle=layout.findViewById(R.id.alert_title);
        alertTitle.setText("Remove Marker");
        Button confirmButton=layout.findViewById(R.id.alert_confirm);
        confirmButton.setText("Remove");
        confirmButton.setTextColor(mColorDanger);
        View titleLogo=layout.findViewById(R.id.alert_title_logo);
        titleLogo.setBackground(ResourcesCompat.getDrawable(getResources(),R.drawable.ic_delete_black_24dp,getTheme()));

        Drawable ImageViewDrawable = titleLogo.getBackground();
        DrawableCompat.setTint(ImageViewDrawable, mColorDanger.getDefaultColor() );
        titleLogo.setBackground(ImageViewDrawable);

        layout.findViewById(R.id.alert_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertD.dismiss();
            }
        });

        layout.findViewById(R.id.alert_confirm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeMarker(markerId);
                alertD.dismiss();

            }
        });
    }
    public void removeMarker(final long markerId){
        realm.beginTransaction();
        try {
            Marker tempMarker=markerList.get(markerId);
            if(tempMarker!=null){
                realm.where(MarkerModel.class).equalTo("id",markerId).findAll().deleteAllFromRealm();
                realm.commitTransaction();
                Toast.makeText(mContext, "Removed", Toast.LENGTH_SHORT).show();
                if(speedDialView.getActionItems().contains(editMarkerFab))
                    speedDialView.removeActionItemById(R.id.fab_edit_location);
                if(speedDialView.getActionItems().contains(deleteMarkerFab))
                    speedDialView.removeActionItemById(R.id.fab_delete_location);
                loadMarkers();
            }
        }catch (Exception ex){
            realm.cancelTransaction();
            Toast.makeText(mContext, "Failure : " + ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void editLocation(final long markerId){

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext, R.style.CustomDialog);
        LayoutInflater inflater = LayoutInflater.from(mContext);
        final View layout=inflater.inflate(R.layout.alert_save_location,null);
        builder.setView(layout);

        TextView alertTitle=layout.findViewById(R.id.alert_save_title);
        alertTitle.setText("Edit Selected Location");
        View titleLogo=layout.findViewById(R.id.alert_save_title_logo);
        titleLogo.setBackground(ResourcesCompat.getDrawable(getResources(),R.drawable.ic_mode_edit_black_24dp,getTheme()));

        Drawable ImageViewDrawable = titleLogo.getBackground();
        DrawableCompat.setTint(ImageViewDrawable, mColorAccent.getDefaultColor() );
        titleLogo.setBackground(ImageViewDrawable);

        ((Button)layout.findViewById(R.id.alert_save_marker_button)).setText("Save");
        final AlertDialog alertD=builder.show();

        final AwesomeValidation mAwesomeValidation = new AwesomeValidation(BASIC);
        final EditText mMarkerTitle=layout.findViewById(R.id.alert_save_edit_text);
        mAwesomeValidation.addValidation(mMarkerTitle,"^(?!\\s*$)[\\s\\S]{1,50}$","Title must have less than 50 characters");
        mAwesomeValidation.addValidation(mMarkerTitle,"^(?!\\s*$)[\\s\\S]{1,}$","Title can't be empty");

        try{
            MarkerModel markerModel=realm.where(MarkerModel.class).equalTo("id",markerId).findFirst();
            mMarkerTitle.setText(markerModel.getTitle());
            if(markerModel.getEmoji().equals(" ")){
                ((Switch)layout.findViewById(R.id.alert_save_emoji_enable)).setChecked(false);
                ((EmojiTextView)layout.findViewById(R.id.alert_save_emoji_button)).setText("🏡");
                ((EmojiTextView)layout.findViewById(R.id.alert_save_emoji_preview)).setText(" ");
            }else{
                ((EmojiTextView)layout.findViewById(R.id.alert_save_emoji_button)).setText(markerModel.getEmoji());
                ((EmojiTextView)layout.findViewById(R.id.alert_save_emoji_preview)).setText(markerModel.getEmoji());
            }
            String tempColor=markerModel.getColor();
            if(tempColor.equals("#222f3e") && DARK_THEME)
                tempColor="#f0f6ff";

            defaultSelectedColor=tempColor;
            Drawable buttonDrawable1 = (layout.findViewById(R.id.alert_save_color)).getBackground();
            buttonDrawable1 = DrawableCompat.wrap(buttonDrawable1);
            (layout.findViewById(R.id.alert_save_color)).setBackground(buttonDrawable1);
            DrawableCompat.setTint(buttonDrawable1, Color.parseColor(tempColor));
            //the color is a direct color int and not a color resource
            Drawable ImageViewDrawable1 = (layout.findViewById(R.id.alert_save_marker_body)).getBackground();
            DrawableCompat.setTint(ImageViewDrawable1, Color.parseColor(tempColor));
            (layout.findViewById(R.id.alert_save_marker_body)).setBackground(ImageViewDrawable1);

        }catch (Exception ex){
            Toast.makeText(mContext, "Fail : "+ex.getMessage(), Toast.LENGTH_SHORT).show();
            alertD.dismiss();
        }




        fillColorList();
        layout.findViewById(R.id.alert_save_marker_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mAwesomeValidation.validate()){
                    String emojiText=((EmojiTextView)layout.findViewById(R.id.alert_save_emoji_preview)).getText().toString();
                    //Add the marker
                    String tempColor=defaultSelectedColor;
                    if(defaultSelectedColor.equals("#f0f6ff"))
                        tempColor="#222f3e";
                    updateMarker(markerId,mMarkerTitle.getText().toString(),emojiText,tempColor);
                    alertD.dismiss();

                }
            }
        });

        ((Switch)layout.findViewById(R.id.alert_save_emoji_enable)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked)
                    ((EmojiTextView)layout.findViewById(R.id.alert_save_emoji_preview))
                            .setText(((EmojiTextView)layout.findViewById(R.id.alert_save_emoji_button)).getText().toString());
                else
                    ((EmojiTextView)layout.findViewById(R.id.alert_save_emoji_preview)).setText(" ");
            }
        });



        final EmojiEditText emojiEditText=layout.findViewById(R.id.alert_emojiEditText);
        layout.findViewById(R.id.alert_save_emoji_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                emojiEditText.setVisibility(View.VISIBLE);

                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                LayoutInflater inflater = LayoutInflater.from(mContext);
                final View pickerLayout=inflater.inflate(R.layout.alert_emoji_picker,null);
                builder.setView(pickerLayout);
                final AlertDialog picker=builder.show();

                emojiEditText.setText("");
                EmojiPopup.Builder emojiBuilder=EmojiPopup.Builder.fromRootView(pickerLayout);
                final EmojiPopup emojiPopup=emojiBuilder.build(emojiEditText);
                //Open up emoji keyboard
                emojiPopup.toggle();
                //Hide Default Keyboard
                InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                emojiPopup.toggle();
                //get emoji from edit text to preview
                emojiEditText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override
                    public void afterTextChanged(Editable s) {
                        emojiPopup.dismiss();
                        picker.dismiss();
                        emojiPopup.dismiss();
                    }
                });

                picker.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if(!emojiEditText.getText().toString().equals("")) {
                            ((EmojiTextView) layout.findViewById(R.id.alert_save_emoji_button)).setText(emojiEditText.getText().toString());
                            if(((Switch)layout.findViewById(R.id.alert_save_emoji_enable)).isChecked())
                                ((EmojiTextView) layout.findViewById(R.id.alert_save_emoji_preview)).setText(emojiEditText.getText().toString());
                            else
                                ((EmojiTextView) layout.findViewById(R.id.alert_save_emoji_preview)).setText(" ");
                        }
                        emojiEditText.setVisibility(View.GONE);
                    }
                });
            }
        });

        //fillColorList();//Fill colors
        layout.findViewById(R.id.alert_save_color).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ColorPicker colorPicker = new ColorPicker(MapsActivity.this);
                colorPicker.setOnFastChooseColorListener(new ColorPicker.OnFastChooseColorListener() {
                    @Override
                    public void setOnFastChooseColorListener(int position, int color) {
                        // put code
                        defaultSelectedColor=colorHexList.get(position);
                        Drawable buttonDrawable = (layout.findViewById(R.id.alert_save_color)).getBackground();
                        buttonDrawable = DrawableCompat.wrap(buttonDrawable);
                        (layout.findViewById(R.id.alert_save_color)).setBackground(buttonDrawable);
                        DrawableCompat.setTint(buttonDrawable, color);
                        //the color is a direct color int and not a color resource
                        Drawable ImageViewDrawable = (layout.findViewById(R.id.alert_save_marker_body)).getBackground();
                        DrawableCompat.setTint(ImageViewDrawable, color);
                        (layout.findViewById(R.id.alert_save_marker_body)).setBackground(ImageViewDrawable);
                    }

                    @Override
                    public void onCancel(){
                        // put code
                    }
                })
                        .setColors(colorHexList)
                        .setDefaultColorButton(Color.parseColor(defaultSelectedColor))
                        .setColumns(5)
                        .setRoundColorButton(true)
                        .setColorButtonTickColor(mColorWhite.getDefaultColor())
                        .setTitle("Choose a color for marker")
                        .show();
                colorPicker.getDialogBaseLayout().setBackgroundColor(mColorPrimary.getDefaultColor());
            }
        });
        //builder.setTitle("hello");
        alertD.findViewById(R.id.alert_save_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertD.dismiss();
            }
        });
    }
    public void updateMarker(long markerId, String markerTitle,String markerEmoji,String markerColor){
        realm.beginTransaction();
        try{
            MarkerModel markerModel=realm.where(MarkerModel.class).equalTo("id",markerId).findFirst();
            markerModel.setTitle(markerTitle);
            if(markerEmoji.equals(""))
                markerModel.setEmoji(" ");
            else
                markerModel.setEmoji(markerEmoji);
            markerModel.setColor(markerColor);
            realm.commitTransaction();
            loadMarkers();
            Toast.makeText(mContext, "Updated", Toast.LENGTH_SHORT).show();
            notifyChange();
        }catch (Exception ex){
            realm.cancelTransaction();
            Toast.makeText(mContext, "Fail : "+ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void loadMarkers(){
        if(markerList.size()>0){
            for (Map.Entry<Long,Marker> markerEntry : markerList.entrySet()){
                if(markerEntry.getValue()!=null){
                    markerEntry.getValue().remove();
                }
            }
            markerList.clear();
        }

        try{
            RealmResults<MarkerModel> markerModels=realm.where(MarkerModel.class).sort("id",Sort.DESCENDING).findAll();

            for(MarkerModel markerModel: markerModels){
                View marker = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.custom_marker_layout, null);
                ((EmojiTextView)marker.findViewById(R.id.emojicon_icon)).setText(markerModel.getEmoji());

                Drawable ImageViewDrawable = (marker.findViewById(R.id.custom_marker_background)).getBackground();
                String tempColor=markerModel.getColor();
                if(tempColor.equals("#222f3e") && DARK_THEME)
                    tempColor="#f0f6ff";
                DrawableCompat.setTint(ImageViewDrawable, Color.parseColor(tempColor));
                (marker.findViewById(R.id.custom_marker_background)).setBackground(ImageViewDrawable);

                Marker newMarker=mMap.addMarker(new MarkerOptions().position(new LatLng(markerModel.getLatitude(),markerModel.getLongitude()))
                        .title(markerModel.getTitle())
                        .icon(BitmapDescriptorFactory.fromBitmap(createDrawableFromView(mContext, marker)))
                );
                markerList.put(markerModel.getId(),newMarker);
            }

            MarksListAdapter marksListAdapter=new MarksListAdapter(this,markerModels,DARK_THEME);
            markListRecyclerView.setAdapter(marksListAdapter);
            markListRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));

            slidingPaneLayout.setScrollableView(markListRecyclerView);
            if(markerModels.size()>0)
                noMarkerSaved.setVisibility(View.GONE);
            else
                noMarkerSaved.setVisibility(View.VISIBLE);

        }catch (Exception ex){
            Toast.makeText(mContext, "Fail "+ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }//Load The markers


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMyLocationClickListener(this);
        enableMyLocation();
        mUiSettings = mMap.getUiSettings();
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                View view = ((Activity)mContext).getLayoutInflater()
                        .inflate(R.layout.marker_info_window, null);
                ((TextView)view.findViewById(R.id.info_window_text)).setText(marker.getTitle());
                return view;
            }

            @Override
            public View getInfoContents(Marker marker) {
                return null;
            }
        });

        if(DARK_THEME) {
            try {
                boolean success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(mContext, R.raw.style_json));
                if (!success) {
                    Log.e("PLACE MARK: Debjoy :", "Style parsing failed.");
                }
            } catch (Resources.NotFoundException e) {
                Log.e("PLACE MARK: Debjoy :", "Can't find style. Error: ", e);
            }
        }else{
            try {
                boolean success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(mContext, R.raw.light_style_json));
                if (!success) {
                    Log.e("PLACE MARK: Debjoy :", "Style parsing failed.");
                }
            } catch (Resources.NotFoundException e) {
                Log.e("PLACE MARK: Debjoy :", "Can't find style. Error: ", e);
            }
        }

        mUiSettings.setMapToolbarEnabled(false);
        mUiSettings.setCompassEnabled(true);

        Intent intent=getIntent();
        Uri uri=intent.getData();
        if(uri!=null){
            String schemeSpecificPart=uri.getSchemeSpecificPart();
            String uriFirstPart=schemeSpecificPart.split("[?]")[0];
            double latitudeIntent=Double.parseDouble(uriFirstPart.split(",")[0]);
            double longitudeIntent=Double.parseDouble(uriFirstPart.split(",")[1]);
            LatLng latLng=new LatLng(latitudeIntent,longitudeIntent);
            if(!speedDialView.getActionItems().contains(saveMarkerFab))
                speedDialView.addActionItem(saveMarkerFab);
            if(speedDialView.getActionItems().contains(deleteMarkerFab))
                speedDialView.removeActionItemById(R.id.fab_delete_location);
            if(speedDialView.getActionItems().contains(editMarkerFab))
                speedDialView.removeActionItemById(R.id.fab_edit_location);

            markerId=-1;
            if(mMap.getCameraPosition().zoom<=15.0)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,17),1000,null);
            else
                mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng),400,null);

            View marker = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.custom_marker_layout, null);

            ((EmojiTextView)marker.findViewById(R.id.emojicon_icon)).setText("");
            if(selectedMarker!=null)
                selectedMarker.remove();
            selectedMarker=mMap.addMarker(new MarkerOptions().position(latLng)
                    .title("Untitled")
                    .icon(BitmapDescriptorFactory.fromBitmap(createDrawableFromView(mContext, marker)))
            );
            selectedMarker.setDraggable(true);
        }

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if(!speedDialView.getActionItems().contains(saveMarkerFab))
                    speedDialView.addActionItem(saveMarkerFab);
                if(speedDialView.getActionItems().contains(deleteMarkerFab))
                    speedDialView.removeActionItemById(R.id.fab_delete_location);
                if(speedDialView.getActionItems().contains(editMarkerFab))
                    speedDialView.removeActionItemById(R.id.fab_edit_location);

                markerId=-1;

                View marker = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.custom_marker_layout, null);

                ((EmojiTextView)marker.findViewById(R.id.emojicon_icon)).setText("");
                if(selectedMarker!=null)
                selectedMarker.remove();
                selectedMarker=mMap.addMarker(new MarkerOptions().position(latLng)
                        .title("Untitled")
                        .icon(BitmapDescriptorFactory.fromBitmap(createDrawableFromView(mContext, marker)))
                );
                selectedMarker.setDraggable(true);
                //mMap.animateCamera(CameraUpdateFactory.newLatLng(selectedMarker.getPosition()),400,null);
            }
        });


        LatLng home=null;
        if(isLocationEnabled(this)){
            home = getMyLocation();

            if(home!=null)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(home, Math.min(mMap.getMaxZoomLevel(),17)));

        }
        speedDialView.addActionItem(currentMarkerFab);
        mMap.setOnMarkerClickListener(this);
        loadMarkers();//Load the markers
        if(home==null && markerList.size()>0){
            ArrayList<Marker> tempMarkers=new ArrayList<>(markerList.values());
            Marker tempMarker=tempMarkers.get(0);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(tempMarker.getPosition().latitude,tempMarker.getPosition().longitude)
                    , Math.min(mMap.getMaxZoomLevel(),17)));
        }
        mMap.setOnMapLoadedCallback(this);

        List<Address> addressList=null;
        Geocoder geocoder=new Geocoder(MapsActivity.this);
        try {
            addressList=geocoder.getFromLocationName("Damodar",5);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.i("DEBJOY",addressList.size()+"");
    }
    @Override
    public void onMapLoaded() {
        mMapCover.setVisibility(View.GONE);
    }

    public LatLng getMyLocation(){
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        String locationProvider = LocationManager.NETWORK_PROVIDER;
        @SuppressLint("MissingPermission")
        android.location.Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
        if(lastKnownLocation!=null){
            double userLat = lastKnownLocation.getLatitude();
            double userLong = lastKnownLocation.getLongitude();
            //Toast.makeText(this, userLat+", "+userLong, Toast.LENGTH_SHORT).show();
            LatLng current = new LatLng(userLat,userLong);
            return current;
        }else{
            return null;
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if(markerList.containsValue(marker)){
            markerId=markerList.inverse().get(marker);
            speedDialView.addActionItem(deleteMarkerFab);
            speedDialView.addActionItem(editMarkerFab);
            speedDialView.removeActionItemById(R.id.fab_selected_location);
            if(selectedMarker!=null)
            selectedMarker.remove();
        }

        return false;
    }//When a Marker is clicked

    @Override
    public boolean onMyLocationButtonClick() {
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        if(!isLocationEnabled(mContext)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext, R.style.CustomDialog);
            LayoutInflater inflater = LayoutInflater.from(mContext);
            final View layout=inflater.inflate(R.layout.alert_template,null);
            builder.setView(layout);
            final AlertDialog alertD=builder.show();
            ((TextView)layout.findViewById(R.id.alert_text)).setText("You have to enable your location services first.");
            TextView alertTitle=layout.findViewById(R.id.alert_title);
            alertTitle.setText("Enable Location");
            Button confirmButton=layout.findViewById(R.id.alert_confirm);
            confirmButton.setText("OK");
            confirmButton.setTextColor(mColorTextTitle);
            View titleLogo=layout.findViewById(R.id.alert_title_logo);
            titleLogo.setBackground(ResourcesCompat.getDrawable(getResources(),R.drawable.ic_info_black_24dp,getTheme()));

            Drawable ImageViewDrawable = titleLogo.getBackground();
            DrawableCompat.setTint(ImageViewDrawable, mColorAccent.getDefaultColor() );
            titleLogo.setBackground(ImageViewDrawable);

            layout.findViewById(R.id.alert_close).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    alertD.dismiss();
                }
            });

            layout.findViewById(R.id.alert_confirm).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    alertD.dismiss();
                }
            });

        }


        return false;
    }//When the  get location button is clicked
    @Override
    public void onMyLocationClick(@NonNull Location location) {

        View marker = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.custom_marker_layout, null);
            /*TextView numTxt = (TextView) marker.findViewById(R.id.num_txt);
            numTxt.setText("27");*/
        ((EmojiTextView)marker.findViewById(R.id.emojicon_icon)).setText("");
        if(selectedMarker!=null)
            selectedMarker.remove();
        selectedMarker=mMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(),location.getLongitude()))
                .title("Untitled")
                .icon(BitmapDescriptorFactory.fromBitmap(createDrawableFromView(mContext, marker)))
        );
        selectedMarker.setDraggable(true);
    } // When my current location is clicked show marker on it

    public void focusMarker(long markerId){
        Marker tempMarker=markerList.get(markerId);
        if(mMap.getCameraPosition().zoom<=15.0)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(tempMarker.getPosition(),17),1000,null);
        else
            mMap.animateCamera(CameraUpdateFactory.newLatLng(tempMarker.getPosition()),400,null);
        slidingPaneLayout.setPanelState(COLLAPSED);
        tempMarker.showInfoWindow();
    }

    public void notifyChange(){
        notifyItem.setVisibility(View.VISIBLE);
    }
    // [START maps_check_location_permission_result]
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults, Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation();
        } else {
            // Permission was denied. Display an error message
            // [START_EXCLUDE]
            // Display the missing permission error dialog when the fragments resume.
            // [END_EXCLUDE]
        }
    }
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (mMap != null) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
            }

        } else {
            // Permission to access the location is missing. Show rationale and request permission
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        }
        // [END maps_check_location_permission]
    }
    public static Boolean isLocationEnabled(Context context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
// This is new method provided in API 28
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            return lm.isLocationEnabled();
        } else {
// This is Deprecated in API 28
            int mode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE,
                    Settings.Secure.LOCATION_MODE_OFF);
            return  (mode != Settings.Secure.LOCATION_MODE_OFF);

        }
    }
    // [END maps_check_location_permission_result]

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_items, menu);
        MenuItem themeToggle=menu.findItem(R.id.menu_item);
        MenuItem searchToggle=menu.findItem(R.id.search_icon);
        if(DARK_THEME) {
            themeToggle.setIcon(ContextCompat.getDrawable(mContext, R.drawable.ic_brightness_7_black_24dp));
            Drawable ImageViewDrawable1 = themeToggle.getIcon();
            DrawableCompat.setTint(ImageViewDrawable1, mColorTextTitle.getDefaultColor());
            themeToggle.setIcon(ImageViewDrawable1);
        }else{
            themeToggle.setIcon(ContextCompat.getDrawable(mContext, R.drawable.ic_brightness_2_black_24dp));
            Drawable ImageViewDrawable1 = themeToggle.getIcon();
            DrawableCompat.setTint(ImageViewDrawable1, mColorTextTitle.getDefaultColor());
            themeToggle.setIcon(ImageViewDrawable1);
        }

        Drawable ImageViewDrawable1 = searchToggle.getIcon();
        DrawableCompat.setTint(ImageViewDrawable1, mColorTextTitle.getDefaultColor());
        searchToggle.setIcon(ImageViewDrawable1);
        final SearchView searchView = (SearchView)searchToggle.getActionView();
        searchView.setQueryHint("Search Location");
        searchView.color
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Toast.makeText(mContext, "query", Toast.LENGTH_SHORT).show();
                searchView.setIconified(true);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.menu_item:
                SharedPreferences sharedpreferences = getSharedPreferences("PlaceMarkApp", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putBoolean("DarkTheme", !DARK_THEME);
                editor.commit();
                Intent intent = new Intent(mContext, MapsActivity.class);
                mContext.startActivity(intent);
                if (mContext instanceof Activity) {
                    ((Activity) mContext).finish();
                }


                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    public static Bitmap createDrawableFromView(Context context, View view) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        view.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        view.measure(displayMetrics.widthPixels, displayMetrics.heightPixels);
        view.layout(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels);
        view.buildDrawingCache();
        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);

        return bitmap;
    }//for creating a custom marker; covert view to marker
    public  void fillColorList(){
        colorHexList.clear();
        if(DARK_THEME)
            colorHexList.add("#f0f6ff");
        else
            colorHexList.add("#222f3e");
        colorHexList.add("#c51162");
        colorHexList.add("#aa00ff");
        colorHexList.add("#6200ea");
        colorHexList.add("#304ffe");
        colorHexList.add("#0091ea");
        colorHexList.add("#00b8d4");
        colorHexList.add("#00bfa5");
        colorHexList.add("#00c853");
        colorHexList.add("#64dd17");
        colorHexList.add("#aeea00");
        colorHexList.add("#ffd600");
        colorHexList.add("#ffab00");
        colorHexList.add("#ff6d00");
        colorHexList.add("#dd2c00");

    }

    public void setUpFabItems(){
        Drawable drawable = AppCompatResources.getDrawable(mContext, R.drawable.marker);
        saveMarkerFab=new SpeedDialActionItem.Builder(R.id.fab_selected_location, drawable)
                .setLabelColor(mColorTextTitle.getDefaultColor())
                .setLabel("Mark Selected Location")
                .setFabImageTintColor(mColorAccent.getDefaultColor())
                .setFabBackgroundColor(mColorWhite.getDefaultColor())
                .setLabelBackgroundColor(mColorWhite.getDefaultColor())
                .create();

        Drawable drawable1 = AppCompatResources.getDrawable(mContext, R.drawable.ic_delete_black_24dp);
        deleteMarkerFab= new SpeedDialActionItem.Builder(R.id.fab_delete_location, drawable1)
                        .setLabelColor(mColorDanger.getDefaultColor())
                        .setLabel("Remove")
                        .setFabImageTintColor(mColorDanger.getDefaultColor())
                        .setFabBackgroundColor(mColorWhite.getDefaultColor())
                        .setLabelBackgroundColor(mColorWhite.getDefaultColor())
                        .create();
        Drawable drawable2 = AppCompatResources.getDrawable(mContext, R.drawable.ic_mode_edit_black_24dp);
        editMarkerFab=new SpeedDialActionItem.Builder(R.id.fab_edit_location, drawable2)
                        .setLabelBackgroundColor(mColorAccent.getDefaultColor())
                        .setLabelColor(Color.WHITE)
                        .setLabel("Edit")
                        .setFabImageTintColor(mColorAccent.getDefaultColor())
                        .setFabBackgroundColor(mColorWhite.getDefaultColor())
                        .create();
        Drawable drawable3 = AppCompatResources.getDrawable(mContext, R.drawable.ic_my_location_black_24dp);
        currentMarkerFab=new SpeedDialActionItem.Builder(R.id.fab_current_location, drawable3)
                .setLabelColor(mColorTextTitle.getDefaultColor())
                .setLabel("Mark Your Location")
                .setFabImageTintColor(mColorText.getDefaultColor())
                .setFabBackgroundColor(mColorWhite.getDefaultColor())
                .setLabelBackgroundColor(mColorWhite.getDefaultColor())
                .create();

    }
    public void setColorsToColorStateList(){
        final TypedArray a = mContext.obtainStyledAttributes(new int[] { R.attr.colorAccent });
        @StyleRes final int styleResId = a.getResourceId(0, 0);
        a.recycle();
        TypedArray temp = mContext.obtainStyledAttributes(styleResId, new int[] { android.R.attr.colorPrimary });
        mColorPrimary = temp.getColorStateList(0);
        temp.recycle();
        temp = mContext.obtainStyledAttributes(styleResId, new int[] { android.R.attr.colorAccent });
        mColorAccent = temp.getColorStateList(0);
        temp.recycle();
        temp = mContext.obtainStyledAttributes(styleResId, new int[] { R.attr.colorTextTitle });
        mColorTextTitle = temp.getColorStateList(0);
        temp.recycle();
        temp = mContext.obtainStyledAttributes(styleResId, new int[] { R.attr.colorBackground });
        mColorBackground = temp.getColorStateList(0);
        temp.recycle();
        temp = mContext.obtainStyledAttributes(styleResId, new int[] { R.attr.colorSuccess});
        mColorSuccess = temp.getColorStateList(0);
        temp.recycle();
        temp = mContext.obtainStyledAttributes(styleResId, new int[] { R.attr.colorGrey});
        mColorGrey = temp.getColorStateList(0);
        temp.recycle();
        temp = mContext.obtainStyledAttributes(styleResId, new int[] { R.attr.colorMarker});
        mColorMarker = temp.getColorStateList(0);
        temp.recycle();
        temp = mContext.obtainStyledAttributes(styleResId, new int[] { R.attr.colorText});
        mColorText = temp.getColorStateList(0);
        temp.recycle();
        temp = mContext.obtainStyledAttributes(styleResId, new int[] { R.attr.colorWhite});
        mColorWhite = temp.getColorStateList(0);
        temp.recycle();
        temp = mContext.obtainStyledAttributes(styleResId, new int[] { R.attr.colorDanger});
        mColorDanger = temp.getColorStateList(0);
        temp.recycle();
    }
}
