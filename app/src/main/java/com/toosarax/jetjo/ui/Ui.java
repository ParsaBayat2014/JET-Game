package com.toosarax.jetjo.ui;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public final class Ui {
    public static final int BG=0xff080d19,CARD=0xff111e2a,LINE=0xff28404a,
            TEXT=0xffeff8f4,MUTED=0xff9baebd,LIME=0xff8cffb9,GOLD=0xffffd779;
    public final Activity activity;
    public final Words words;
    public final Typeface regular,bold;

    public Ui(Activity activity, Words words) {
        this.activity=activity; this.words=words;
        regular=Typeface.createFromAsset(activity.getAssets(),"fonts/regular.ttf");
        bold=Typeface.createFromAsset(activity.getAssets(),"fonts/bold.ttf");
    }
    public int dp(float n) {
        return (int)(n*activity.getResources().getDisplayMetrics().density+.5f);
    }
    public LinearLayout column() {
        LinearLayout layout=new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutDirection(words.rtl()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);
        return layout;
    }
    public GradientDrawable panel(int color,int radius) {
        GradientDrawable drawable=new GradientDrawable();
        drawable.setColor(color); drawable.setCornerRadius(dp(radius)); drawable.setStroke(dp(1),LINE);
        return drawable;
    }
    public TextView text(String content,float size,int color,boolean strong) {
        TextView view=new TextView(activity);
        view.setText(content); view.setTextSize(size); view.setTextColor(color);
        view.setTypeface(strong?bold:regular);
        view.setLayoutDirection(words.rtl()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);
        view.setTextDirection(words.rtl()?View.TEXT_DIRECTION_RTL:View.TEXT_DIRECTION_LTR);
        view.setGravity(Gravity.START); view.setLineSpacing(dp(2),1); view.setIncludeFontPadding(false);
        return view;
    }
    public void add(LinearLayout parent,View child,int height,int bottom) {
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,height<0?height:dp(height));
        params.bottomMargin=dp(bottom); parent.addView(child,params);
    }
    public TextView label(LinearLayout parent,String content,float size,int color,int bottom) {
        TextView label=text(content,size,color,false); add(parent,label,-2,bottom); return label;
    }
    public Button button(String title,boolean primary,Runnable click) {
        Button button=new Button(activity);
        button.setText(title); button.setTextSize(15); button.setTypeface(bold); button.setAllCaps(false);
        button.setTextColor(new ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},
                new int[]{primary?0xff335444:MUTED,primary?BG:TEXT}));
        button.setGravity(Gravity.CENTER);
        button.setLayoutDirection(words.rtl()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);
        button.setTextDirection(words.rtl()?View.TEXT_DIRECTION_RTL:View.TEXT_DIRECTION_LTR);
        button.setMinimumWidth(0); button.setMinWidth(0); button.setMinimumHeight(dp(54));
        button.setPadding(dp(12),dp(10),dp(12),dp(10));
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(0x446ae4ff),
                panel(primary?LIME:CARD,16),null));
        button.setBackgroundTintList(null);
        button.setOnClickListener(view->click.run());
        return button;
    }
    public LinearLayout card(LinearLayout parent) {
        LinearLayout card=column();
        card.setPadding(dp(18),dp(17),dp(18),dp(17)); card.setBackground(panel(CARD,20));
        add(parent,card,-2,12); return card;
    }
    public void progress(LinearLayout parent,int value,int max) {
        ProgressBar bar=new ProgressBar(activity,null,android.R.attr.progressBarStyleHorizontal);
        bar.setMax(Math.max(1,max)); bar.setProgress(Math.max(0,Math.min(value,bar.getMax())));
        bar.setProgressTintList(ColorStateList.valueOf(LIME));
        bar.setProgressBackgroundTintList(ColorStateList.valueOf(LINE));
        add(parent,bar,7,10);
    }
    /** Paired actions stack on narrow phones or when large accessible text is selected. */
    public void pair(LinearLayout parent,View first,View second) {
        boolean stack=activity.getResources().getConfiguration().screenWidthDp<340
                || activity.getResources().getConfiguration().fontScale>1.3f;
        LinearLayout row=new LinearLayout(activity);
        row.setLayoutDirection(parent.getLayoutDirection());
        row.setOrientation(stack?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL);
        row.setBaselineAligned(false);
        if(stack) {
            LinearLayout.LayoutParams top=new LinearLayout.LayoutParams(-1,-2);
            top.bottomMargin=dp(8); row.addView(first,top);
            row.addView(second,new LinearLayout.LayoutParams(-1,-2));
        } else {
            LinearLayout.LayoutParams leading=new LinearLayout.LayoutParams(0,-2,1);
            leading.setMarginEnd(dp(8)); row.addView(first,leading);
            row.addView(second,new LinearLayout.LayoutParams(0,-2,1));
        }
        add(parent,row,-2,10);
    }
    /** Small reusable number tile for campaign, records, inventory and result screens. */
    public LinearLayout metric(String label,String value,int accent) {
        LinearLayout tile=column();
        tile.setPadding(dp(13),dp(12),dp(13),dp(12)); tile.setBackground(panel(CARD,16));
        add(tile,text(value,23,accent,true),-2,4); add(tile,text(label,11,MUTED,false),-2,0);
        return tile;
    }
    public TextView chip(String label,int accent) {
        TextView chip=text(label,11,accent,true);
        chip.setPadding(dp(11),dp(6),dp(11),dp(6)); chip.setBackground(panel(CARD,12));
        return chip;
    }
}
