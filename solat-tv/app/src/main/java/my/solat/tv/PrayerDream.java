package my.solat.tv;
import android.service.dreams.DreamService;
public final class PrayerDream extends DreamService {
    private PrayerController controller;
    private PrayerScreen screen;
    @Override public void onAttachedToWindow() {
        super.onAttachedToWindow(); setInteractive(false); setFullscreen(true); setScreenBright(false);
        screen = new PrayerScreen(this); setContentView(screen);
    }
    @Override public void onDreamingStarted() {
        super.onDreamingStarted();
        controller = new PrayerController(this, screen, true); controller.start();
    }
    @Override public void onDreamingStopped() { if (controller != null) controller.stop(); super.onDreamingStopped(); }
    @Override public void onDetachedFromWindow() { if (controller != null) controller.stop(); super.onDetachedFromWindow(); }
}
