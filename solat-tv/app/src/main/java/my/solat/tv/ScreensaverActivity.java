package my.solat.tv;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;

/** Manual screensaver entry point for the Glass TV home-screen Back shortcut. */
public final class ScreensaverActivity extends Activity {
    private PrayerController controller;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
            | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override public void onResume() {
        super.onResume();
        PrayerScreen screen = new PrayerScreen(this);
        setContentView(screen);
        controller = new PrayerController(this, screen, true);
        controller.start();
    }

    @Override public void onPause() {
        if (controller != null) { controller.stop(); controller = null; }
        super.onPause();
    }

    @Override public void onBackPressed() { finish(); }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_ESCAPE:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                // Consume both halves, so dismissal cannot activate a launcher tile.
                if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) finish();
                return true;
            default:
                // Volume/mute and system Home/Power retain their normal behavior.
                return super.dispatchKeyEvent(event);
        }
    }
}
