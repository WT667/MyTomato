package com.tomatocleaner.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textview.MaterialTextView;
import com.tomatocleaner.R;

/**
 * 主控面板：三个目标 App 的功能开关总览。
 *
 * 开关写入 SharedPreferences，Hook 进程通过 XSharedPreferences 读取。
 */
public class MainActivity extends AppCompatActivity {

    private static final String PREFS = "tomato_cleaner_prefs";

    private SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sp = getSharedPreferences(PREFS, MODE_PRIVATE);

        // 番茄畅听
        MaterialSwitch swListenVip = findViewById(R.id.sw_listen_vip);
        MaterialSwitch swListenAd = findViewById(R.id.sw_listen_ad);
        swListenVip.setChecked(sp.getBoolean("listen_vip", true));
        swListenAd.setChecked(sp.getBoolean("listen_ad", true));
        swListenVip.setOnCheckedChangeListener((b, v) -> save("listen_vip", v));
        swListenAd.setOnCheckedChangeListener((b, v) -> save("listen_ad", v));

        // 番茄小说
        MaterialSwitch swNovelTab = findViewById(R.id.sw_novel_tab);
        MaterialSwitch swNovelFloat = findViewById(R.id.sw_novel_float);
        swNovelTab.setChecked(sp.getBoolean("novel_tab", true));
        swNovelFloat.setChecked(sp.getBoolean("novel_float", true));
        swNovelTab.setOnCheckedChangeListener((b, v) -> save("novel_tab", v));
        swNovelFloat.setOnCheckedChangeListener((b, v) -> save("novel_float", v));

        // 红果短剧
        MaterialSwitch swHongguoTab = findViewById(R.id.sw_hongguo_tab);
        MaterialSwitch swHongguoFloat = findViewById(R.id.sw_hongguo_float);
        swHongguoTab.setChecked(sp.getBoolean("hongguo_tab", true));
        swHongguoFloat.setChecked(sp.getBoolean("hongguo_float", true));
        swHongguoTab.setOnCheckedChangeListener((b, v) -> save("hongguo_tab", v));
        swHongguoFloat.setOnCheckedChangeListener((b, v) -> save("hongguo_float", v));

        // 状态提示
        MaterialTextView tvStatus = findViewById(R.id.tv_status);
        tvStatus.setOnClickListener(v ->
                Toast.makeText(this, R.string.status_hint, Toast.LENGTH_LONG).show());
    }

    private void save(String key, boolean value) {
        sp.edit().putBoolean(key, value).apply();
    }
}
