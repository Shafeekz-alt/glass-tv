package my.solat.tv;
import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;
public final class PreviewActivity extends Activity {
    private PrayerController controller;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }
    @Override public void onResume() {
        super.onResume(); PrayerScreen screen = new PrayerScreen(this); setContentView(screen);
        controller = new PrayerController(this, screen, false); controller.start();
    }
    @Override public void onPause() { if (controller != null) controller.stop(); super.onPause(); }
}
