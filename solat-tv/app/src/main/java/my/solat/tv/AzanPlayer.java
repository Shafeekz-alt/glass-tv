package my.solat.tv;

import android.content.*;
import android.media.*;
import android.net.Uri;

/** Media volume is respected. No alarms, wake locks or playback outside an active screensaver. */
public final class AzanPlayer {
    private final Context context;
    private final AudioManager audio;
    private AudioFocusRequest focus;
    private MediaPlayer player;
    private Runnable error;
    public AzanPlayer(Context context) { this.context = context; audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE); }
    public static boolean available(Context c) {
        return !c.getSharedPreferences("settings", Context.MODE_PRIVATE).getString("audioUri", "").isEmpty()
            || c.getResources().getIdentifier("azan", "raw", c.getPackageName()) != 0;
    }
    public void play(Runnable onError) {
        stop(); error = onError;
        AudioAttributes attrs = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build();
        focus = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attrs).setOnAudioFocusChangeListener(change -> {
                if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) stop();
            }).build();
        if (audio.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) { fail(); return; }
        try {
            player = new MediaPlayer(); player.setAudioAttributes(attrs);
            String custom = context.getSharedPreferences("settings", Context.MODE_PRIVATE).getString("audioUri", "");
            if (!custom.isEmpty()) player.setDataSource(context, Uri.parse(custom));
            else {
                int resource = context.getResources().getIdentifier("azan", "raw", context.getPackageName());
                if (resource == 0) throw new IllegalStateException("No audio configured");
                player.setDataSource(context, Uri.parse("android.resource://" + context.getPackageName() + "/" + resource));
            }
            player.setOnPreparedListener(MediaPlayer::start);
            player.setOnCompletionListener(p -> stop());
            player.setOnErrorListener((p, what, extra) -> { fail(); return true; });
            player.prepareAsync();
        } catch (Exception ex) { fail(); }
    }
    private void fail() { Runnable callback = error; stop(); if (callback != null) callback.run(); }
    public void stop() {
        if (player != null) { player.release(); player = null; }
        if (focus != null) { audio.abandonAudioFocusRequest(focus); focus = null; }
        error = null;
    }
}
