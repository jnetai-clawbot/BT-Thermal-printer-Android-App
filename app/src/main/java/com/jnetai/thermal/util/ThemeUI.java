package com.jnetai.thermal.util;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

public class ThemeUI {
    public static final int PRIMARY = 0xFF1A73E8;
    public static final int PRIMARY_LIGHT = 0xFF8AB4F8;
    public static final int BG_DARK = 0xFF1E1E1E;
    public static final int SURFACE = 0xFF2D2D2D;
    public static final int CARD = 0xFF3C3C3C;
    public static final int TEXT_WHITE = 0xFFFFFFFF;
    public static final int TEXT_GREY = 0xFFCCCCCC;
    public static final int TEXT_MUTED = 0xFFB0B0B0;

    public static ScrollView scrollRoot(Context ctx) {
        ScrollView sv = new ScrollView(ctx);
        sv.setBackgroundColor(BG_DARK);
        return sv;
    }

    public static LinearLayout vertical(Context ctx) {
        LinearLayout ll = new LinearLayout(ctx);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setPadding(dp(ctx, 16), dp(ctx, 8), dp(ctx, 16), dp(ctx, 120));
        return ll;
    }

    public static TextView header(Context ctx, String text) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextSize(22);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextColor(PRIMARY_LIGHT);
        tv.setPadding(0, dp(ctx, 12), 0, dp(ctx, 12));
        return tv;
    }

    public static TextView subHeader(Context ctx, String text) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextSize(17);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextColor(TEXT_WHITE);
        tv.setPadding(0, dp(ctx, 10), 0, dp(ctx, 6));
        return tv;
    }

    public static TextView label(Context ctx, String text) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextSize(14);
        tv.setTextColor(TEXT_GREY);
        tv.setPadding(0, dp(ctx, 10), 0, dp(ctx, 2));
        return tv;
    }

    public static TextView info(Context ctx, String text) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextSize(13);
        tv.setTextColor(TEXT_MUTED);
        tv.setPadding(0, 0, 0, dp(ctx, 6));
        return tv;
    }

    public static TextView selectionLabel(Context ctx, String initial) {
        TextView tv = new TextView(ctx);
        tv.setText(initial != null && !initial.isEmpty() ? "Selected: " + initial : "Selected: none");
        tv.setTextSize(12);
        tv.setTextColor(PRIMARY_LIGHT);
        tv.setPadding(dp(ctx, 4), 0, dp(ctx, 4), dp(ctx, 8));
        return tv;
    }

    public static Button button(Context ctx, String text) {
        Button b = new Button(ctx);
        b.setText(text);
        b.setTextColor(TEXT_WHITE);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setBackgroundColor(PRIMARY);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        b.setLayoutParams(lp);
        return b;
    }

    public static Button secondaryButton(Context ctx, String text) {
        Button b = button(ctx, text);
        b.setBackgroundColor(SURFACE);
        return b;
    }

    public static EditText input(Context ctx, String value, int inputType) {
        EditText et = new EditText(ctx);
        et.setText(value == null ? "" : value);
        et.setTextColor(TEXT_WHITE);
        et.setTextSize(15);
        et.setHintTextColor(TEXT_MUTED);
        et.setBackground(rounded(CARD, dp(ctx, 8)));
        et.setPadding(dp(ctx, 14), dp(ctx, 12), dp(ctx, 14), dp(ctx, 12));
        if (inputType > 0) et.setInputType(inputType);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        et.setLayoutParams(lp);
        return et;
    }

    public static GradientDrawable rounded(int color, int radius) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(radius);
        return gd;
    }

    public static Spinner spinnerWithListener(final Context ctx, String[] items, String selected, AdapterView.OnItemSelectedListener listener) {
        Spinner sp = new Spinner(ctx);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(ctx, android.R.layout.simple_spinner_item, items) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = convertView instanceof TextView ? (TextView) convertView : new TextView(ctx);
                tv.setText(getItem(position));
                tv.setTextSize(16);
                tv.setTextColor(ThemeUI.TEXT_WHITE);
                tv.setTypeface(Typeface.DEFAULT_BOLD);
                tv.setPadding(ThemeUI.dp(ctx, 12), ThemeUI.dp(ctx, 12), ThemeUI.dp(ctx, 12), ThemeUI.dp(ctx, 12));
                tv.setBackground(ThemeUI.rounded(ThemeUI.CARD, 0));
                return tv;
            }
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView tv = convertView instanceof TextView ? (TextView) convertView : new TextView(ctx);
                tv.setText(getItem(position));
                tv.setTextSize(16);
                tv.setTextColor(ThemeUI.TEXT_WHITE);
                tv.setTypeface(Typeface.DEFAULT_BOLD);
                tv.setPadding(ThemeUI.dp(ctx, 12), ThemeUI.dp(ctx, 12), ThemeUI.dp(ctx, 12), ThemeUI.dp(ctx, 12));
                tv.setBackground(ThemeUI.rounded(ThemeUI.SURFACE, 0));
                return tv;
            }
        };
        sp.setAdapter(adapter);
        for (int i = 0; i < items.length && selected != null; i++) {
            if (items[i].equals(selected)) {
                sp.setSelection(i);
                break;
            }
        }
        if (listener != null) sp.setOnItemSelectedListener(listener);
        return sp;
    }

    public static Switch toggle(Context ctx, boolean checked, android.widget.CompoundButton.OnCheckedChangeListener l) {
        Switch sw = new Switch(ctx);
        sw.setChecked(checked);
        if (l != null) sw.setOnCheckedChangeListener(l);
        return sw;
    }

    public static LinearLayout switchRow(Context ctx, String label, boolean checked, android.widget.CompoundButton.OnCheckedChangeListener l) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(ctx, 6), 0, dp(ctx, 6));
        TextView tv = new TextView(ctx);
        tv.setText(label);
        tv.setTextSize(14);
        tv.setTextColor(TEXT_WHITE);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Switch sw = toggle(ctx, checked, l);
        row.addView(tv);
        row.addView(sw);
        return row;
    }

    public static void hideKeyboard(Context ctx, View v) {
        InputMethodManager imm = (InputMethodManager) ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && v != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    public static int dp(Context ctx, int dp) {
        return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
    }
}