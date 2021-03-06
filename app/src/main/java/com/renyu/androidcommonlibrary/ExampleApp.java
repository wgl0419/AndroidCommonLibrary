package com.renyu.androidcommonlibrary;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.readystatesoftware.chuck.ChuckInterceptor;
import com.renyu.androidcommonlibrary.dbhelper.PlainTextDBHelper;
import com.renyu.commonlibrary.commonutils.ImagePipelineConfigUtils;
import com.renyu.commonlibrary.commonutils.Utils;
import com.renyu.commonlibrary.commonutils.sonic.SonicRuntimeImpl;
import com.renyu.commonlibrary.network.HttpsUtils;
import com.renyu.commonlibrary.network.Retrofit2Utils;
import com.renyu.commonlibrary.params.InitParams;
import com.tencent.smtt.sdk.QbSdk;
import com.tencent.sonic.sdk.SonicConfig;
import com.tencent.sonic.sdk.SonicEngine;
import com.tencent.wcdb.database.SQLiteDatabase;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import okhttp3.OkHttpClient;

/**
 * Created by renyu on 2016/12/26.
 */

public class ExampleApp extends MultiDexApplication {

    public PlainTextDBHelper dbHelper;
    public SQLiteDatabase db;

    public ArrayList<String> openClassNames;

    @Override
    public void onCreate() {
        super.onCreate();

        openClassNames = new ArrayList<>();

        String processName= Utils.getProcessName(android.os.Process.myPid());
        if (processName.equals(getPackageName())) {
            // 初始化网络请求
            Retrofit2Utils retrofit2Utils=Retrofit2Utils.getInstance("http://www.mocky.io/v2/");
            OkHttpClient.Builder baseBuilder=new OkHttpClient.Builder()
//                    .addInterceptor(new TokenInterceptor(this))
                    .addInterceptor(new ChuckInterceptor(this))
                    .readTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .connectTimeout(10, TimeUnit.SECONDS);
            //https默认信任全部证书
            HttpsUtils.SSLParams sslParams= HttpsUtils.getSslSocketFactory(null, null, null);
            baseBuilder.hostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String s, SSLSession sslSession) {
                    return true;
                }
            }).sslSocketFactory(sslParams.sSLSocketFactory, sslParams.trustManager);
            retrofit2Utils.addBaseOKHttpClient(baseBuilder.build());
            retrofit2Utils.baseBuild();
            OkHttpClient.Builder imageUploadOkBuilder=new OkHttpClient.Builder()
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .connectTimeout(30, TimeUnit.SECONDS);
            retrofit2Utils.addImageOKHttpClient(imageUploadOkBuilder.build());
            retrofit2Utils.imageBuild();

            // 初始化工具库
            com.blankj.utilcode.util.Utils.init(this);

            // 初始化fresco
            Fresco.initialize(this, ImagePipelineConfigUtils.getDefaultImagePipelineConfig(this));

            // 初始化数据库
            if (dbHelper!=null && db!=null && db.isOpen()) {
                db.close();
                db=null;
                dbHelper=null;
            }
            dbHelper=new PlainTextDBHelper(getApplicationContext());
            dbHelper.setWriteAheadLoggingEnabled(true);
            db=dbHelper.getWritableDatabase();

            // 初始化相关配置参数
            // 项目根目录
            // 请注意修改xml文件夹下filepaths.xml中的external-path节点，此值需与ROOT_PATH值相同，作为fileprovider使用
            InitParams.ROOT_PATH= Environment.getExternalStorageDirectory().getPath()+ File.separator + "example";
            // 项目图片目录
            InitParams.IMAGE_PATH= InitParams.ROOT_PATH + File.separator + "image";
            // 项目文件目录
            InitParams.FILE_PATH= InitParams.ROOT_PATH + File.separator + "file";
            // 项目热修复目录
            InitParams.HOTFIX_PATH= InitParams.ROOT_PATH + File.separator + "hotfix";
            // 项目日志目录
            InitParams.LOG_PATH= InitParams.ROOT_PATH + File.separator + "log";
            InitParams.LOG_NAME= "example_log";
            // 缓存目录
            InitParams.CACHE_PATH= InitParams.ROOT_PATH + File.separator + "cache";
            // fresco缓存目录
            InitParams.FRESCO_CACHE_NAME= "fresco_cache";

            // 初始化Sonic
            if (!SonicEngine.isGetInstanceAllowed()) {
                SonicEngine.createInstance(new SonicRuntimeImpl(this), new SonicConfig.Builder().build());
            }

            // x5内核初始化接口
            QbSdk.PreInitCallback cb = new QbSdk.PreInitCallback() {

                @Override
                public void onViewInitFinished(boolean arg0) {
                    //x5內核初始化完成的回调，为true表示x5内核加载成功，否则表示x5内核加载失败，会自动切换到系统内核。
                    Log.d(getPackageName(), " x5加载成功：" + arg0);
                }

                @Override
                public void onCoreInitFinished() {

                }
            };
            QbSdk.initX5Environment(getApplicationContext(),  cb);

            registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
                @Override
                public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

                }

                @Override
                public void onActivityStarted(Activity activity) {
                    openClassNames.add(activity.getLocalClassName());
                }

                @Override
                public void onActivityResumed(Activity activity) {

                }

                @Override
                public void onActivityPaused(Activity activity) {

                }

                @Override
                public void onActivityStopped(Activity activity) {
                    openClassNames.remove(activity.getLocalClassName());
                }

                @Override
                public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

                }

                @Override
                public void onActivityDestroyed(Activity activity) {

                }
            });
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        MultiDex.install(base);
    }
}
