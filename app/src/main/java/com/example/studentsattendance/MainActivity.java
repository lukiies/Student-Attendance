package com.example.studentsattendance;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MainActivity extends AppCompatActivity {
    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        EnsurePermissions.checkAndRequestPermissions(this);

        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        viewPager.setAdapter(new ScreenSlidePagerAdapter(this));

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    if (position == 0) tab.setText("Map");
                    else if (position == 1) tab.setText("Log");
                    else tab.setText("Profile");
                }
        ).attach();

        if (getIntent().getBooleanExtra("show_profile_tab", false)) {
            viewPager.setCurrentItem(2);
            Toast.makeText(this, "Please fill in all Profile fields", Toast.LENGTH_LONG).show();
        }
    }

    private static class ScreenSlidePagerAdapter extends FragmentStateAdapter {
        //This parameter indicates which activity the ViewPager is set to.
        public ScreenSlidePagerAdapter(FragmentActivity fa) {
            super(fa);
        }

        @Override
        public Fragment createFragment(int position) {
            if (position == 0) return new MapFragment();
            else if (position == 1) return new LogFragment();
            else return new ProfileFragment();
        }

        //TabLayoutMediator calls this method to know how many menu positions there are
        @Override
        public int getItemCount() {
            return 3;
        }
    }
}