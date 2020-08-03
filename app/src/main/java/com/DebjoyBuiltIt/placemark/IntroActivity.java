package com.DebjoyBuiltIt.placemark;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.chyrta.onboarder.OnboarderActivity;
import com.chyrta.onboarder.OnboarderPage;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class IntroActivity extends OnboarderActivity {
    List<OnboarderPage> onboarderPages;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        onboarderPages = new ArrayList<OnboarderPage>();

        // Create your first page
        OnboarderPage onboarderPage1 = new OnboarderPage("Mark any place",
                "Bookmark any place you want. Be it your own or someone else's.",
                R.drawable.ic_undraw_my_location_f9pr);
        OnboarderPage onboarderPage2 = new OnboarderPage("Search a location",
                "You can search a location by clicking on the search icon on top of app bar.",
                R.drawable.ic_undraw_location_search_bqps);
        OnboarderPage onboarderPage3 = new OnboarderPage("Use emoticons",
                "You can emoticons and colors from inbuilt color pallet to customise your marker.",
                R.drawable.ic_undraw_smiley_face_lmgm);
        OnboarderPage onboarderPage4 = new OnboarderPage("Day/Night theme",
                "You can toggle between day or night mode with a click of a button.",
                R.drawable.ic_undraw_dark_mode_2xam);

        // You can define title and description colors (by default white)
        onboarderPage1.setTitleColor(R.color.colorText);
        onboarderPage1.setDescriptionColor(R.color.colorTextTitle);
        onboarderPage1.setBackgroundColor(R.color.colorWhite);

        onboarderPage2.setTitleColor(R.color.colorText);
        onboarderPage2.setDescriptionColor(R.color.colorTextTitle);
        onboarderPage2.setBackgroundColor(R.color.colorWhite);

        onboarderPage3.setTitleColor(R.color.colorText);
        onboarderPage3.setDescriptionColor(R.color.colorTextTitle);
        onboarderPage3.setBackgroundColor(R.color.colorWhite);

        onboarderPage4.setTitleColor(R.color.colorText);
        onboarderPage4.setDescriptionColor(R.color.colorTextTitle);
        onboarderPage4.setBackgroundColor(R.color.colorWhite);
        // Don't forget to set background color for your page

        shouldUseFloatingActionButton(true);
        setActiveIndicatorColor(R.color.colorAccent);
        FloatingActionButton fab= (FloatingActionButton) findViewById(R.id.fab);
        fab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorAccent)));

        // Don't forget to set background color for your page

        // Add your pages to the list
        onboarderPages.add(onboarderPage1);
        onboarderPages.add(onboarderPage2);
        onboarderPages.add(onboarderPage3);
        onboarderPages.add(onboarderPage4);

        // And pass your pages to 'setOnboardPagesReady' method
        setOnboardPagesReady(onboarderPages);

    }

    @Override
    public void onSkipButtonPressed() {
        // Optional: by default it skips onboarder to the end
        super.onSkipButtonPressed();
        // Define your actions when the user press 'Skip' button
    }

    @Override
    public void onFinishButtonPressed() {
        // Define your actions when the user press 'Finish' button
        SharedPreferences sharedpreferences = getSharedPreferences("PlaceMarkApp", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putBoolean("ONBOARD", true);
        editor.apply();
        startActivity(new Intent(this, MapsActivity.class));
    }
}
