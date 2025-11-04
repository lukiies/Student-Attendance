package com.example.studentsattendance;

import android.os.Bundle;

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

        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        //metodo que decide o que mostrar em cada aba do menu
        viewPager.setAdapter(new ScreenSlidePagerAdapter(this));

        //verifica qual o texto eu vou mostrar no menu dependendo da posicao que esta
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(position == 0 ? "Map" : "Profile")
        ).attach();
    }

    private static class ScreenSlidePagerAdapter extends FragmentStateAdapter {
        //parametro indica qual activity o viewPager esta setado
        public ScreenSlidePagerAdapter(FragmentActivity fa) {
            super(fa);
        }

        @Override
        public Fragment createFragment(int position) {
            //if (position == 0)
                return new MapFragment();
            //else
                //return new ProfileFragment();
        }

        //TabLayoutMediator chama esse metodo pra saber quantas posicoes tem
        @Override
        public int getItemCount() {
            return 2;
        }
    }
}