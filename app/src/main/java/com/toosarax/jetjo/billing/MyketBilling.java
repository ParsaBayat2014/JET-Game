package com.toosarax.jetjo.billing;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import com.toosarax.jetjo.BuildConfig;
import ir.myket.billingclient.IabHelper;
import ir.myket.billingclient.util.*;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

/** One in-flight operation, lifecycle guards, verified entitlements and live store prices. */
public final class MyketBilling {
    public enum State { NOT_CONFIGURED, STORE_MISSING, CONNECTING, SYNCING, READY, PURCHASING, ERROR, CLOSED }
    public interface Listener {void changed();void message(String en,String fa);}
    private final Activity activity;
    private final Listener listener;
    public final Entitlements entitlements;
    public State state=State.NOT_CONFIGURED;
    private IabHelper helper;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Map<String,SkuDetails> details=new HashMap<>();
    private int generation;
    private final Runnable timeout=()->fail("Store connection timed out. Please retry.","اتصال به فروشگاه طول کشید؛ دوباره تلاش کنید.");
    public MyketBilling(Activity activity,Listener listener){this.activity=activity;this.listener=listener;entitlements=new Entitlements(activity);}
    public boolean configured(){
        if(!activity.getPackageName().equals("com.neonrush.arcade"))return false;
        try{return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.decode(BuildConfig.IAB_PUBLIC_KEY,Base64.DEFAULT)))!=null;}catch(Exception e){return false;}
    }
    public boolean busy(){return state==State.CONNECTING||state==State.SYNCING||state==State.PURCHASING;}
    public String price(String sku){SkuDetails d=details.get(sku);return d==null?null:d.getPrice();}
    public boolean canBuy(String sku){return state==State.READY&&details.containsKey(sku)&&!entitlements.owned(sku);}
    private void changed(State s){state=s;listener.changed();}
    public void connect(){
        if(busy()||state==State.CLOSED)return;
        if(!configured()){changed(State.NOT_CONFIGURED);return;}
        try{activity.getPackageManager().getApplicationInfo("ir.mservices.market",0);}catch(Exception absent){changed(State.STORE_MISSING);return;}
        disposeHelper();final int current=++generation;
        changed(State.CONNECTING);handler.postDelayed(timeout,30000);
        try{
            helper=new IabHelper(activity,BuildConfig.IAB_PUBLIC_KEY);helper.enableDebugLogging(false);
            helper.startSetup(result->onUi(()->{
                if(current!=generation||state==State.CLOSED)return;
                handler.removeCallbacks(timeout);
                if(result==null||!result.isSuccess()){fail("Could not connect to Myket. Try again.","اتصال به مایکت برقرار نشد؛ دوباره تلاش کنید.");return;}
                restore();
            }));
        }catch(RuntimeException e){fail("Myket is unavailable. Update Myket and retry.","مایکت در دسترس نیست؛ آن را به‌روز کرده و دوباره تلاش کنید.");}
    }
    public void restore(){
        if(state==State.CLOSED||state==State.PURCHASING||state==State.SYNCING)return;
        if(helper==null){connect();return;}
        final int current=generation;changed(State.SYNCING);handler.postDelayed(timeout,30000);
        try{
            helper.queryInventoryAsync(true,Products.IDS,(result,inventory)->onUi(()->{
                if(current!=generation||state==State.CLOSED)return;
                handler.removeCallbacks(timeout);
                if(result==null||!result.isSuccess()||inventory==null){fail("Could not restore purchases. Your saved items remain available.","بازیابی خریدها انجام نشد؛ آیتم‌های ذخیره‌شده همچنان فعال‌اند.");return;}
                if(!entitlements.reconcile(inventory.getAllPurchases())){fail("Purchase verification or saving failed. Please retry.","تأیید یا ذخیرهٔ خرید انجام نشد؛ دوباره تلاش کنید.");return;}
                details.clear();for(String id:Products.IDS){SkuDetails d=inventory.getSkuDetails(id);if(d!=null)details.put(id,d);}
                changed(State.READY);
            }));
        }catch(RuntimeException e){fail("The store is busy. Please retry.","فروشگاه مشغول است؛ دوباره تلاش کنید.");}
    }
    public void buy(String sku){
        if(!canBuy(sku))return;
        final int current=generation;changed(State.PURCHASING);
        // Deliberate user confirmation happens in MainActivity before this method.
        try{
            helper.launchPurchaseFlow(activity,sku,(result,purchase)->onUi(()->{
                if(current!=generation||state==State.CLOSED)return;
                changed(State.READY);
                if(result==null){fail("No purchase response. Restore purchases before retrying.","پاسخ خرید دریافت نشد؛ ابتدا بازیابی خریدها را بزنید.");return;}
                if(result.getResponse()==IabHelper.BILLING_RESPONSE_RESULT_USER_CANCELED||result.getResponse()==IabHelper.IABHELPER_USER_CANCELLED){listener.message("Purchase cancelled.","خرید لغو شد.");return;}
                if(result.getResponse()==IabHelper.BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED){restore();return;}
                if(!result.isSuccess()||purchase==null||!sku.equals(purchase.getSku())||!entitlements.grant(purchase)){
                    listener.message("Purchase not confirmed. Restore purchases if you were charged.","خرید تأیید نشد. اگر مبلغ کسر شده، بازیابی خریدها را بزنید.");restore();return;
                }
                listener.message("Collection unlocked. Thank you!","مجموعه فعال شد؛ سپاس از حمایت شما!");listener.changed();
            }),"");
        }catch(RuntimeException e){fail("Purchase did not start. Restore purchases and retry.","خرید شروع نشد؛ خریدها را بازیابی کرده و دوباره تلاش کنید.");}
    }
    public void recover(){if(state==State.CLOSED)return;generation++;handler.removeCallbacks(timeout);disposeHelper();state=State.ERROR;connect();}
    private void onUi(Runnable r){activity.runOnUiThread(r);}
    private void fail(String en,String fa){handler.removeCallbacks(timeout);generation++;disposeHelper();changed(State.ERROR);listener.message(en,fa);}
    private void disposeHelper(){if(helper!=null){try{helper.dispose();}catch(RuntimeException ignored){}helper=null;}}
    public void close(){generation++;handler.removeCallbacksAndMessages(null);disposeHelper();state=State.CLOSED;}
}

