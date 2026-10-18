package com.toosarax.jetjo.billing;
import android.content.Context;
import android.util.AtomicFile;
import android.util.Base64;
import com.toosarax.jetjo.BuildConfig;
import ir.myket.billingclient.util.Purchase;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Stores signed receipts, never an editable purchased=true flag. Revalidates on launch. */
public final class Entitlements {
    private final AtomicFile file;
    private Map<String,Purchase> receipts=new HashMap<>();
    private final String packageName;
    public Entitlements(Context context){
        file=new AtomicFile(new File(context.getFilesDir(),"myket-receipts-v1.json"));
        packageName=context.getPackageName();
        try{
            JSONArray a=new JSONArray(new String(file.readFully(),StandardCharsets.UTF_8));
            for(int i=0;i<a.length();i++){JSONObject j=a.getJSONObject(i);Purchase p=new Purchase("inapp",j.getString("data"),j.getString("signature"));if(valid(p))receipts.put(p.getSku(),p);}
        }catch(Exception ignored){/* Missing or invalid receipts never unlock paid content. */}
    }
    public boolean owned(String sku){return receipts.containsKey(sku);}
    public boolean skin(int skin){int pack=Products.packForSkin(skin);return pack<0||owned(Products.IDS.get(pack));}
    public boolean valid(Purchase p){
        try{
            if(p==null||!"inapp".equals(p.getItemType())||!Products.IDS.contains(p.getSku())||!packageName.equals(p.getPackageName())||p.getPurchaseState()!=0||p.getToken().isEmpty())return false;
            JSONObject j=new JSONObject(p.getOriginalJson());
            if(!j.has("purchaseState")||!j.has("packageName")||!j.has("productId"))return false;
            return ReceiptCrypto.verify(Base64.decode(BuildConfig.IAB_PUBLIC_KEY,Base64.DEFAULT),p.getOriginalJson(),Base64.decode(p.getSignature(),Base64.DEFAULT));
        }catch(Exception invalid){return false;}
    }
    public boolean grant(Purchase p){
        if(!valid(p))return false;
        Map<String,Purchase> next=new HashMap<>(receipts);next.put(p.getSku(),p);return commit(next);
    }
    /** Called only after a fully successful inventory query. Offline failures keep cached receipts. */
    public boolean reconcile(List<Purchase> purchases){
        Map<String,Purchase> next=new HashMap<>();
        for(Purchase p:purchases){if(Products.IDS.contains(p.getSku())){if(!valid(p))return false;next.put(p.getSku(),p);}}
        return commit(next);
    }
    private boolean commit(Map<String,Purchase> next){
        FileOutputStream out=null;
        try{
            JSONArray a=new JSONArray();
            for(Purchase p:next.values()){JSONObject j=new JSONObject();j.put("data",p.getOriginalJson());j.put("signature",p.getSignature());a.put(j);}
            out=file.startWrite();out.write(a.toString().getBytes(StandardCharsets.UTF_8));file.finishWrite(out);receipts=next;return true;
        }catch(Exception failure){if(out!=null)file.failWrite(out);return false;}
    }
}

