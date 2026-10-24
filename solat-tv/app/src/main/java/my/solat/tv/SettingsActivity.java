package my.solat.tv;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

public final class SettingsActivity extends Activity {
    private static final int PICK_AUDIO = 10;
    private SharedPreferences prefs;
    private AzanPlayer player;
    private JakimRepository repository;
    private TextView audioStatus, dataStatus;
    private LinearLayout column;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        player = new AzanPlayer(this); repository = new JakimRepository(this);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(5, 17, 20));
        column = new LinearLayout(this); column.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(36); column.setPadding(pad, pad, pad, pad);
        scroll.addView(column); setContentView(scroll);
        label("Waktu Solat", 30);
        label("Kuala Terengganu • TRG01\nJadual: e-Solat JAKIM • Waktu Malaysia", 18);
        label("Azan berbunyi apabila screensaver aktif. Tekan butang remote untuk keluar dan menghentikan audio. TV perlu hidup; screensaver ini tidak membunyikan azan ketika aplikasi lain digunakan.", 16);
        dataStatus = label("Memuatkan jadual JAKIM…", 16);
        audioStatus = label("", 16);
        updateAudioStatus();
        Switch sound = new Switch(this); sound.setText("Bunyi azan semasa screensaver"); sound.setTextColor(Color.WHITE);
        sound.setPadding(0, dp(10), 0, dp(10)); sound.setChecked(prefs.getBoolean("sound", true));
        sound.setOnCheckedChangeListener((button, checked) -> { prefs.edit().putBoolean("sound", checked).apply(); if (!checked) player.stop(); });
        column.addView(sound);
        button("Pratonton paparan", () -> startActivity(new Intent(this, PreviewActivity.class)));
        button("Uji bunyi azan", () -> {
            if (!AzanPlayer.available(this)) { toast("Pilih fail audio azan dahulu"); return; }
            player.play(() -> toast("Audio tidak dapat dimainkan. Pilih semula fail audio dan semak kelantangan media."));
        });
        button("Hentikan ujian audio", player::stop);
        button("Pilih fail audio azan sendiri", () -> {
            Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("audio/*").addCategory(Intent.CATEGORY_OPENABLE);
            pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            try { startActivityForResult(pick, PICK_AUDIO); }
            catch (ActivityNotFoundException ex) { toast("Pemilih fail tidak tersedia pada TV ini. Gunakan audio yang disertakan."); }
        });
        button("Gunakan audio yang disertakan", () -> { prefs.edit().remove("audioUri").apply(); updateAudioStatus(); });
        button("Kemas kini jadual JAKIM", () -> refresh(true));
        button("Foto masjid dan kredit", () -> new AlertDialog.Builder(this).setTitle("Foto masjid")
            .setMessage(PhotoRepository.credits(this)).setPositiveButton("Tutup", null).show());
        button("Buka tetapan screensaver TV", () -> {
            try { startActivity(new Intent(Settings.ACTION_DREAM_SETTINGS)); }
            catch (ActivityNotFoundException ex) { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
        });
        button("Sumber dan kredit", () -> new AlertDialog.Builder(this).setTitle("Sumber dan kredit")
            .setMessage("Waktu solat: Portal e-Solat, Jabatan Kemajuan Islam Malaysia (JAKIM)\nhttps://www.e-solat.gov.my/\nZon TRG01: Kuala Terengganu, Marang, Kuala Nerus.\n\nAplikasi peribadi ini bukan aplikasi rasmi JAKIM.\n\nJika audio disertakan: The Adhan – Muslim Call to Prayer – Aaqib Azeez. Pengarang: Atcovi. Wikimedia Commons. CC BY-SA 4.0, tanpa pengubahsuaian.\nhttps://commons.wikimedia.org/wiki/File:The_Adhan_-_Muslim_Call_to_Prayer_-_Aaqib_Azeez.mp3\nhttps://creativecommons.org/licenses/by-sa/4.0/\n\nFail azan pilihan sendiri kekal milik pemiliknya.")
            .setPositiveButton("Tutup", null).show());
        refresh(false);
    }
    private void refresh(boolean force) {
        repository.refresh(force, (days, status) -> { dataStatus.setText(status); });
    }
    private void updateAudioStatus() {
        String uri = prefs.getString("audioUri", "");
        audioStatus.setText(!uri.isEmpty() ? "Audio: fail azan pilihan anda" : AzanPlayer.available(this) ? "Audio azan disertakan • gunakan kelantangan media TV" : "Audio belum disertakan • pilih fail azan sebelum mengaktifkan bunyi");
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == PICK_AUDIO && result == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                prefs.edit().putString("audioUri", uri.toString()).apply(); updateAudioStatus();
            } catch (SecurityException ex) { toast("Fail tidak memberi akses kekal. Pilih fail daripada storan tempatan TV."); }
        }
    }
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private TextView label(String text, int size) {
        TextView view = new TextView(this); view.setText(text); view.setTextSize(size);
        view.setTextColor(Color.rgb(207, 220, 215)); view.setPadding(0, 0, 0, dp(14));
        column.addView(view); return view;
    }
    private void button(String name, Runnable action) {
        Button b = new Button(this); b.setText(name); b.setAllCaps(false);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.bottomMargin = dp(8); column.addView(b, p); b.setOnClickListener(v -> action.run());
    }
    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
    @Override public void onStop() { player.stop(); super.onStop(); }
    @Override public void onDestroy() { repository.close(); player.stop(); super.onDestroy(); }
}
