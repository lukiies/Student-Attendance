package com.example.studentsattendance;

import android.content.Context;
import android.media.MediaPlayer;

public class SoundUtils {
    public static void playCheckinSound(Context context) {
        try {
            MediaPlayer mp = MediaPlayer.create(context, R.raw.checkin_sound);
            if (mp != null) {
                mp.setOnCompletionListener(MediaPlayer::release);
                mp.start();
            }
        } catch (Exception ignored) {}
    }
}
