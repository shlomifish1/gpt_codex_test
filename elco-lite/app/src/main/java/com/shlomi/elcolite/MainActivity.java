package com.shlomi.elcolite;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.ConsumerIrManager;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import java.util.ArrayList;

/**
 * Independent ELCO Lite proof-of-concept. No network, analytics or permissions.
 * IR formats are Electra/AUX and Airwell RC04/RC08W, not guaranteed for every ELCO AC.
 * IR timing based on openly documented IRremoteESP8266 implementations.
 */
public final class MainActivity extends Activity {
    private static final int BG = 0xFF111927, SURFACE = 0xFF1C293C;
    private static final int WHITE = 0xFFF6FAFF, ACCENT = 0xFF5ED1F2;
    private static final int SECONDARY = 0xFFA9BBD3;
    private ConsumerIrManager emitter;
    private TextView degreeText, statusText;
    private Spinner modelPicker;
    private boolean powered = false;
    private int temperature = 24, mode = 1, fan = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        emitter = (ConsumerIrManager) getSystemService(Context.CONSUMER_IR_SERVICE);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        final ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(26), dp(22), dp(24));
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        scroll.addView(root);

        TextView title = label("ELCO Lite", 29, WHITE, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(50)));
        TextView subtitle = label("שלט קל • אינפרה־אדום", 15, SECONDARY, false);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle);

        gap(root, 28);
        modelPicker = new Spinner(this);
        String[] models = new String[] {
            "דגם 1 • Electra / AUX",
            "דגם 2 • Airwell RC04 / RC08W"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
            this, android.R.layout.simple_spinner_dropdown_item, models);
        modelPicker.setAdapter(adapter);
        modelPicker.setBackgroundColor(0xFFE6F5FF);
        root.addView(modelPicker, new LinearLayout.LayoutParams(-1, dp(52)));

        gap(root, 24);
        degreeText = label(temperature + "°", 72, WHITE, true);
        degreeText.setGravity(Gravity.CENTER);
        root.addView(degreeText, new LinearLayout.LayoutParams(-1, dp(112)));

        TextView tempLabel = label("טמפרטורה", 14, SECONDARY, false);
        tempLabel.setGravity(Gravity.CENTER);
        root.addView(tempLabel);
        gap(root, 12);

        LinearLayout buttons = row();
        button(buttons, "−", 1, false);
        button(buttons, "+", 2, false);
        root.addView(buttons);
        gap(root, 14);

        LinearLayout pRow = row();
        button(pRow, "⏻  הפעלה / כיבוי", 0, true);
        root.addView(pRow);
        gap(root, 16);

        LinearLayout modes = row();
        button(modes, "❄ קירור", 3, false);
        button(modes, "☀ חימום", 4, false);
        root.addView(modes);
        gap(root, 10);

        LinearLayout other = row();
        button(other, "מאוורר", 5, false);
        button(other, "אוטומטי", 6, false);
        root.addView(other);

        gap(root, 20);
        statusText = label("", 14, SECONDARY, false);
        statusText.setGravity(Gravity.CENTER);
        root.addView(statusText, new LinearLayout.LayoutParams(-1, -2));
        statusText.setText(emitter != null && emitter.hasIrEmitter()
            ? "בחר דגם ולחץ על הפעלה. אם אין תגובה, נסה דגם אחר."
            : "לא זוהה משדר IR דרך ממשק Android במכשיר זה.");
        setContentView(scroll);
    }

    private LinearLayout row() {
        LinearLayout result = new LinearLayout(this);
        result.setOrientation(LinearLayout.HORIZONTAL);
        return result;
    }

    private GradientDrawable pill(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(18));
        return d;
    }

    private void button(LinearLayout parent, String text, final int action, boolean prominent) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(text);
        b.setTextColor(prominent ? BG : WHITE);
        b.setTextSize(17);
        b.setTypeface(null, Typeface.BOLD);
        b.setBackground(pill(prominent ? ACCENT : SURFACE));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(64), 1);
        lp.setMargins(dp(5), dp(3), dp(5), dp(3));
        parent.addView(b, lp);
        b.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { action(action); }
        });
    }

    private void action(int action) {
        boolean toggle = false;
        switch(action) {
            case 0: powered = !powered; toggle = true; break;
            case 1: temperature = Math.max(16, temperature - 1); powered = true; break;
            case 2: temperature = Math.min(30, temperature + 1); powered = true; break;
            case 3: mode = 1; powered = true; break;
            case 4: mode = 2; powered = true; break;
            case 5: fan = (fan + 1) % 4; powered = true; break;
            case 6: mode = 0; powered = true; break;
        }
        degreeText.setText(temperature + "°");
        send(toggle);
    }

    private void send(boolean toggle) {
        if (emitter == null || !emitter.hasIrEmitter()) {
            statusText.setText("אין גישה למשדר IR דרך Android. בדוק את אפליקציית השלט המובנית.");
            return;
        }
        try {
            int[] wave = modelPicker.getSelectedItemPosition() == 0
                ? electraWave()
                : airwellWave(toggle);
            emitter.transmit(38000, wave);
            statusText.setText("נשלח אות IR • " + (powered ? "פעיל" : "כבוי")
                + " • אם אין תגובה, נסה את הדגם השני");
        } catch (Exception ex) {
            statusText.setText("שליחת IR נכשלה: " + ex.getClass().getSimpleName());
        }
    }

    /** Electra/AUX: 13-byte frame, each byte LSB first, checksum byte-sum. */
    private int[] electraWave() {
        int[] state = new int[13];
        state[0] = 0xC3;
        state[1] = ((temperature - 8) << 3) | 7; // temp and vertical swing off
        state[2] = (7 << 5); // horizontal swing off
        state[4] = 5 << 5;   // fan auto
        state[6] = (mode == 1 ? 1 : mode == 2 ? 4 : 0) << 5;
        state[9] = powered ? 0x20 : 0;
        state[11] = 0x08; // light toggle disabled
        int sum = 0;
        for (int j=0; j<12; j++) sum += state[j];
        state[12] = sum & 255;
        int[] wave = new int[2 + (13 * 8 * 2) + 2];
        int pos = 0;
        wave[pos++] = 9166; wave[pos++] = 4470;
        for (int b : state) {
            for (int bit=0; bit<8; bit++) {
                wave[pos++] = 646;
                wave[pos++] = ((b & (1<<bit)) == 0) ? 547 : 1647;
            }
        }
        wave[pos++] = 646; wave[pos] = 12000;
        return wave;
    }

    /** Airwell RC04/RC08W: 34-bit Manchester, G.E. Thomas inverted. */
    private int[] airwellWave(boolean toggle) {
        long data = 0x140500002L;
        long mask = (15L << 19) | (3L << 28) | (7L << 30) | (1L << 33);
        data &= ~mask;
        int airwellMode = mode == 1 ? 1 : mode == 2 ? 2 : 3;
        data |= ((long)(temperature - 15) << 19);
        data |= ((long)fan << 28);
        data |= ((long)airwellMode << 30);
        if (toggle) data |= 1L << 33;
        ArrayList<Integer> a = new ArrayList<Integer>(300);
        for (int repeat=0; repeat<3; repeat++) {
            pulse(a, true, 2850); pulse(a, false, 2850);
            for (int bit=33; bit>=0; bit--) {
                boolean one = ((data >>> bit) & 1L) != 0;
                pulse(a, !one, 950);
                pulse(a, one, 950);
            }
        }
        pulse(a, true, 3800);
        pulse(a, false, 12000);
        int[] output = new int[a.size()];
        for (int i=0; i<a.size(); i++) output[i] = a.get(i);
        return output;
    }

    private static void pulse(ArrayList<Integer> data, boolean mark, int micros) {
        int size = data.size();
        if (((size % 2) == 0) == mark) data.add(micros);
        else data.set(size - 1, data.get(size - 1) + micros);
    }

    private TextView label(String text, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }
    private void gap(LinearLayout parent, int dp) {
        View view = new View(this);
        parent.addView(view, new LinearLayout.LayoutParams(1, dp(dp)));
    }
    private int dp(int px) {
        return Math.round(px * getResources().getDisplayMetrics().density);
    }
}
