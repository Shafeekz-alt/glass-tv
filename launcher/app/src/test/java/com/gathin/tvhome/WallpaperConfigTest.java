package com.gathin.tvhome;

import static org.junit.Assert.*;

import android.content.Context;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk = 28)
public class WallpaperConfigTest {
    private Context context;
    private File overlay;

    @Before public void prepare() {
        context = RuntimeEnvironment.getApplication();
        File dir = Config.dir(context);
        dir.mkdirs();
        overlay = new File(dir, Config.FILE_NAME);
        overlay.delete();
    }

    @After public void clean() {
        overlay.delete();
    }

    private Config load(String json) throws Exception {
        Files.write(overlay.toPath(), json.getBytes(StandardCharsets.UTF_8));
        return Config.load(context);
    }

    @Test public void defaultsToFiveSecondsAndKenBurns() {
        Config config = Config.load(context);
        assertEquals(5_000L, config.wallpaperIntervalMs);
        assertTrue(config.kenBurns);
    }

    @Test public void explicitThemeIntervalsAndMotionSwitchStillWork() throws Exception {
        Config config = load("{\"wallpaper_seconds\":5,\"wallpaper_minutes\":30,\"ken_burns\":false}");
        assertEquals(5_000L, config.wallpaperIntervalMs);
        assertFalse(config.kenBurns);
        assertEquals(120_000L, load("{\"wallpaper_minutes\":2}").wallpaperIntervalMs);
        assertEquals(0L, load("{\"wallpaper_minutes\":0}").wallpaperIntervalMs);
    }

    @Test public void invalidThemeValuesKeepTheNewDefaults() throws Exception {
        Config config = load("{\"wallpaper_seconds\":\"fast\",\"wallpaper_minutes\":-1,\"ken_burns\":\"yes\"}");
        assertEquals(5_000L, config.wallpaperIntervalMs);
        assertTrue(config.kenBurns);
    }
}
