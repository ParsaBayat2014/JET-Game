package com.toosarax.jetjo.ui;

import com.toosarax.jetjo.data.Profile;
import java.text.NumberFormat;
import java.util.Locale;

public final class Words {
    private final Profile profile;
    private NumberFormat format;
    public Words(Profile p){profile=p;refresh();}
    public void refresh(){format=NumberFormat.getIntegerInstance(new Locale(profile.language));}
    public boolean rtl(){return profile.language.equals("fa");}
    public String t(String en,String fa){return rtl()?fa:en;}
    public String t(String[] pair){return pair[rtl()?1:0];}
    public String n(long n){return format.format(n);}
}
