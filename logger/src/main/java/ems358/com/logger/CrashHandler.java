package ems358.com.logger;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by hcqi on.
 * Des:
 * Date: 2017/8/2
 */

public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private boolean mInitialized;
    private String defaultDir;
    private String dir = "crash";
    private String versionName;
    private int versionCode;
    private Adapter mAdapter;
    private final String FILE_SEP = System.getProperty("file.separator");
    private final Format FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    private String CRASH_HEAD;

    private Thread.UncaughtExceptionHandler DEFAULT_UNCAUGHT_EXCEPTION_HANDLER;
    //    private static final Thread.UncaughtExceptionHandler UNCAUGHT_EXCEPTION_HANDLER;
    private Context mContext;

    private static CrashHandler sHandler;
    private ExecutorService mExecutorService;


    public static CrashHandler getInstance() {
        if (sHandler == null) {
            synchronized (CrashHandler.class) {
                if (sHandler == null) {
                    sHandler = new CrashHandler();
                }
            }
        }
        return sHandler;
    }


    private CrashHandler() {
    }

    /**
     * 初始化
     * <p>需添加权限 {@code <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>}</p>
     *
     * @return {@code true}: 初始化成功<br>{@code false}: 初始化失败
     */
    public boolean init(Context context, Adapter adapter) {
        if (mInitialized) {
            return mInitialized = true;
        }
        mContext = context.getApplicationContext();
        mAdapter = adapter;
        initDeviceInfo();
        Thread.setDefaultUncaughtExceptionHandler(this);
        return mInitialized = true;
    }

//    /**
//     * @param crashDir 崩溃文件存储目录
//     */
//    public void setDir(final String crashDir) {
//
//        if (isSpace(crashDir)) {
//            dir = null;
//        } else {
//            dir = crashDir + "/crash/";
//        }
//        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
//                && mContext.getExternalCacheDir() != null)
//            defaultDir = mContext.getExternalCacheDir() + FILE_SEP + "crash" + FILE_SEP;
//        else {
//            defaultDir = mContext.getCacheDir() + FILE_SEP + "crash" + FILE_SEP;
//        }
//    }

    private void initDeviceInfo() {
        try {
            PackageInfo pi = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
            if (pi != null) {
                versionName = pi.versionName;
                versionCode = pi.versionCode;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        CRASH_HEAD = "\n************* Crash Log Head ****************" +
                "\nDevice Manufacturer: " + Build.MANUFACTURER +// 设备厂商
                "\nDevice Model       : " + Build.MODEL +// 设备型号
                "\nAndroid Version    : " + Build.VERSION.RELEASE +// 系统版本
                "\nAndroid SDK        : " + Build.VERSION.SDK_INT +// SDK版本
                "\nApp VersionName    : " + versionName +
                "\nApp VersionCode    : " + versionCode +
                "\nThread id    : " + Thread.currentThread().getId() +
                "\n************* Crash Log Head ****************\n\n";
        DEFAULT_UNCAUGHT_EXCEPTION_HANDLER = Thread.getDefaultUncaughtExceptionHandler();
    }

    private boolean createOrExistsFile(final String filePath) {
        File file = new File(filePath);
        if (file.exists()) return file.isFile();
        if (!createOrExistsDir(file.getParentFile())) return false;
        try {
            return file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean createOrExistsDir(final File file) {
        return file != null && (file.exists() ? file.isDirectory() : file.mkdirs());
    }

    private boolean isSpace(final String s) {
        if (s == null) return true;
        for (int i = 0, len = s.length(); i < len; ++i) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void uncaughtException(Thread t, final Throwable throwable) {
        if (throwable == null) {
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
            return;
        }
        throwable.printStackTrace();
        Date now = new Date(System.currentTimeMillis());
        String fileName = FORMAT.format(now) + ".txt";
//        final String fullPath = (dir == null ? defaultDir : dir) + fileName;
//        if (!createOrExistsFile(fullPath)) return;
        if (mExecutorService == null) {
            mExecutorService = Executors.newSingleThreadExecutor();
        }
        mExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                StringWriter out = new StringWriter();
                PrintWriter pw = new PrintWriter(out);
                pw.write(CRASH_HEAD);
                throwable.printStackTrace(pw);
                Throwable cause = throwable.getCause();
                while (cause != null) {
                    cause.printStackTrace(pw);
                    cause = cause.getCause();
                }
                String s = out.toString();
                if (mAdapter != null) {
                    try {
                        mAdapter.upload(s);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        if (DEFAULT_UNCAUGHT_EXCEPTION_HANDLER != null) {
            DEFAULT_UNCAUGHT_EXCEPTION_HANDLER.uncaughtException(t, throwable);
        } else {
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }
}
