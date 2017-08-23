package ems358.com.logger;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.IntDef;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by hcqi on.
 * Des:log 日志
 * Date: 2017/5/25
 */

public class Logger {
    public static final int V = Log.VERBOSE;
    public static final int D = Log.DEBUG;
    public static final int I = Log.INFO;
    public static final int W = Log.WARN;
    public static final int E = Log.ERROR;
    public static final int A = Log.ASSERT;
    //日志级别
    public static int level = V;

    @IntDef({V, D, I, W, E, A})
    @Retention(RetentionPolicy.SOURCE)
    private @interface TYPE {
    }

    private static final char[] T = new char[]{'V', 'D', 'I', 'W', 'E', 'A'};

    private static final int FILE = 0x10;
    private static final int JSON = 0x20;
    private static final int XML = 0x30;
    private static ExecutorService executor;
    private static String defaultDir;// log默认存储目录
    private static String dir;       // log存储目录

    private static boolean sLogSwitch = true; // log总开关，默认开
    private static boolean sLog2ConsoleSwitch = true; // logcat是否打印，默认打印
    private static String sGlobalTag = null; // log标签
    private static boolean sTagIsSpace = true; // log标签是否为空白
    private static boolean sLogHeadSwitch = true; // log头部开关，默认开
    private static boolean sLog2FileSwitch;// log写入文件开关，默认关
    private static boolean sLogBorderSwitch = true; // log边框开关，默认开
    private static int sConsoleFilter = V;    // log控制台过滤器
    private static int sFileFilter = V;    // log文件过滤器
    private static int sFileDisCardDay = 0;    // log文件过滤器

    private static Context sContext;
    private static final String FILE_SEP = System.getProperty("file.separator");
    private static final String LINE_SEP = System.getProperty("line.separator");
    private static final String TOP_BORDER = "╔═══════════════════════════════════════════════════════════════════════════════════════════════════";
    private static final String LEFT_BORDER = "║ ";
    private static final String BOTTOM_BORDER = "╚═══════════════════════════════════════════════════════════════════════════════════════════════════";
    private static final int MAX_LEN = 4000;
    private static final Format FORMAT_FILE = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    private static final Format FORMAT_CONTENT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS", Locale.getDefault());

    private static final String NULL_TIPS = "Log with null object.";
    private static final String NULL = "null";
    private static final String ARGS = "args";

    private Logger() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    public static void setContext(Context context) {
        sContext = context.getApplicationContext();
    }

    public static class Builder {
        public boolean logSwitch = true;
        public boolean consoleSwitch = true;
        public String tag;
        public boolean logHeadSwitch = true;
        public boolean log2FileSwitch;
        public String dir;
        public File dirFile;
        public int level = V;
        public boolean borderSwitch = true;
        public int consoleFilter = V;
        public int fileFilter = V;
        public int discardDay;

        public Builder() {
            if (defaultDir != null) return;
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                    && sContext.getExternalCacheDir() != null)
                defaultDir = sContext.getExternalCacheDir() + FILE_SEP + "log" + FILE_SEP;
            else {
                defaultDir = sContext.getCacheDir() + FILE_SEP + "log" + FILE_SEP;
            }
        }

        public Builder setLogSwitch(boolean logSwitch) {
            Logger.sLogSwitch = logSwitch;

            return this;
        }

        public Builder setConsoleSwitch(boolean consoleSwitch) {
            Logger.sLog2ConsoleSwitch = consoleSwitch;
            return this;
        }

        public Builder setGlobalTag(final String tag) {
            this.tag = tag;

            return this;
        }

        public Builder setLogHeadSwitch(boolean logHeadSwitch) {
            this.logHeadSwitch = logHeadSwitch;
            return this;
        }

        public Builder setLevel(@TYPE int level) {
            this.level = level;
            return this;
        }

        public Builder setDiscardDay(int discardDay) {
            this.discardDay = discardDay;
            return this;
        }

        public Builder setLog2FileSwitch(boolean log2FileSwitch) {
            this.log2FileSwitch = log2FileSwitch;
            return this;
        }

        public Builder setDir(final String dir) {
            this.dir = dir;
            return this;
        }

        public Builder setDir(final File dir) {
            this.dirFile = dir;
            return this;
        }

        public Builder setBorderSwitch(boolean borderSwitch) {
            this.borderSwitch = borderSwitch;
            return this;
        }

        public Builder setConsoleFilter(@TYPE int consoleFilter) {
            this.consoleFilter = consoleFilter;
            return this;
        }

        public Builder setFileFilter(@TYPE int fileFilter) {
            this.fileFilter = fileFilter;
            return this;
        }

        @Override
        public String toString() {
            return "switch: " + sLogSwitch
                    + LINE_SEP + "console: " + sLog2ConsoleSwitch
                    + LINE_SEP + "tag: " + (sTagIsSpace ? "null" : sGlobalTag)
                    + LINE_SEP + "head: " + sLogHeadSwitch
                    + LINE_SEP + "file: " + sLog2FileSwitch
                    + LINE_SEP + "dir: " + (dir == null ? defaultDir : dir)
                    + LINE_SEP + "border: " + sLogBorderSwitch
                    + LINE_SEP + "consoleFilter: " + T[sConsoleFilter - V]
                    + LINE_SEP + "fileFilter: " + T[sFileFilter - V];
        }
    }

    public static void config(Map<String, String> config) throws Exception {
        Builder builder = new Builder();
        if (config.containsKey("dir")) {
            builder.setDir(config.get("dir"));
        }
        if (config.containsKey("borderSwitch")) {
            builder.setBorderSwitch(Boolean.parseBoolean(config.get("borderSwitch")));
        }
        if (config.containsKey("consoleFilter")) {
            builder.setConsoleFilter(convertType(Integer.parseInt(config.get("consoleFilter"))));
        }
        if (config.containsKey("fileFilter")) {
            builder.setLog2FileSwitch(Boolean.parseBoolean(config.get("log2FileSwitch")));
        }
        if (config.containsKey("log2FileSwitch")) {
            builder.setLogHeadSwitch(Boolean.parseBoolean(config.get("logHeadSwitch")));
        }
        if (config.containsKey("logHeadSwitch")) {
            builder.setLogHeadSwitch(Boolean.parseBoolean(config.get("logHeadSwitch")));
        }
        if (config.containsKey("discardDay")) {
            builder.setDiscardDay(Integer.parseInt(config.get("discardDay")));
        }
        if (config.containsKey("tag")) {
            builder.setGlobalTag(config.get("tag"));
        }
        if (config.containsKey("logSwitch")) {
            builder.setLogSwitch(Boolean.parseBoolean(config.get("logSwitch")));
        }
        if (config.containsKey("consoleSwitch")) {
            builder.setConsoleSwitch(Boolean.parseBoolean(config.get("consoleSwitch")));
        }
        if (config.containsKey("level")) {
            builder.setLevel(convertType(Integer.parseInt(config.get("level"))));
        }
        config(builder);
    }

    private static
    @TYPE
    int convertType(int type) {
        switch (type) {
            case 3:
                return Logger.D;
            case 4:
                return Logger.I;
            case 5:
                return Logger.W;
            case 6:
                return Logger.E;
            case 7:
                return Logger.A;
        }
        return Logger.V;
    }

    public static void config(Builder builder) {
        Logger.sFileFilter = builder.fileFilter;
        Logger.sConsoleFilter = builder.consoleFilter;
        if (builder.dirFile != null) {
            String path = builder.dirFile.getPath();
            if (path.endsWith(FILE_SEP)) {
                path += "log" + FILE_SEP;
            } else {
                path += FILE_SEP + "log" + FILE_SEP;
            }
            Logger.dir = path;

        }
        if (!TextUtils.isEmpty(builder.dir)) {
            if (builder.dir.endsWith(FILE_SEP)) {
                Logger.dir = builder.dir + "log" + FILE_SEP;
            } else {
                Logger.dir = builder.dir + FILE_SEP + "log" + FILE_SEP;
            }
        }
        Logger.sLog2FileSwitch = builder.log2FileSwitch;
        Logger.sLogBorderSwitch = builder.borderSwitch;
        Logger.sLogHeadSwitch = builder.logHeadSwitch;
        Logger.sFileDisCardDay = builder.discardDay;
        if (!TextUtils.isEmpty(builder.tag)) {
            if (isSpace(builder.tag)) {
                Logger.sGlobalTag = "";
                sTagIsSpace = true;
            } else {
                Logger.sGlobalTag = builder.tag;
                sTagIsSpace = false;
            }
        }
        Logger.sLogSwitch = builder.logSwitch;
        Logger.sLog2ConsoleSwitch = builder.consoleSwitch;
        Logger.level = builder.level;
        //删除过期文件
        deleteDisCardFile(new File(Logger.dir));
    }

    private static void deleteDisCardFile(File file) {
        if (file.isDirectory()) {
            for (File fl : file.listFiles()) {
                if (fl.isDirectory()) {
                    deleteDisCardFile(fl);
                } else {
                    String name = fl.getName();
                    String fileName = name.replace(".txt", "");
                    try {
                        Date date = (Date) FORMAT_FILE.parseObject(fileName);
                        long time = date.getTime();
                        if (Logger.sFileDisCardDay > 0
                                && System.currentTimeMillis() - time > 1000 * 60 * 60 * 24 * Logger.sFileDisCardDay) {
                            fl.delete();
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static void v(String contents) {
        log(V, sGlobalTag, contents);
    }

    public static void v(String tag, String... contents) {
        log(V, tag, contents);
    }

    public static void v(@TYPE int type, String tag, String contents, Throwable ex) {
        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        String exceptionStr = writer.toString();
        printWriter.close();
        log(V | type, tag, contents + "\n" + exceptionStr);
    }

    public static void d(String contents) {
        log(D, sGlobalTag, contents);
    }

    public static void d(String tag, String... contents) {
        log(D, tag, contents);
    }

    public static void d(String tag, Object contents, Throwable ex) {
        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        String exceptionStr = writer.toString();
        printWriter.close();
        log(D | D, tag, contents + "\n" + exceptionStr);
    }

    public static void i(String contents) {
        log(I, sGlobalTag, contents);
    }

    public static void i(String tag, String... contents) {
        log(I, tag, contents);
    }

    public static void i(String tag, String contents, Throwable ex) {
        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        String exceptionStr = writer.toString();
        printWriter.close();
        log(I | I, tag, contents + "\n" + exceptionStr);
    }

    public static void w(String contents) {
        log(W, sGlobalTag, contents);
    }

    public static void w(String tag, String... contents) {
        log(W, tag, contents);
    }

    public static void w(String tag, String contents, Throwable ex) {
        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        String exceptionStr = writer.toString();
        printWriter.close();
        log(W | W, tag, contents + "\n" + exceptionStr);
    }

    public static void e(String contents) {
        log(E, sGlobalTag, contents);
    }

    public static void e(String tag, String... contents) {
        log(E, tag, contents);
    }

    public static void e(String tag, String contents, Throwable ex) {
        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        String exceptionStr = writer.toString();
        printWriter.close();
        log(E | E, tag, contents + "\n" + exceptionStr);
    }

    public static void a(String contents) {
        log(A, sGlobalTag, contents);
    }

    public static void a(String tag, String... contents) {
        log(A, tag, contents);
    }

    public static void a(String tag, String contents, Throwable ex) {
        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        String exceptionStr = writer.toString();
        printWriter.close();
        log(A | A, tag, contents + "\n" + exceptionStr);
    }


    public static String zip() {
        removeZipFile();
        if (!TextUtils.isEmpty(dir)) {
            try {
                String zipPath = getZipPath();
                boolean files = ZipUtils.zipFile(dir, zipPath);
                if (files) {
                    return zipPath;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static void removeZipFile() {
        if (!TextUtils.isEmpty(dir)) {
            File file = new File(getZipPath());
            if (file.exists()) {
                file.delete();
            }
        }
    }

    private static String getZipPath() {
        String s = Environment.getExternalStorageDirectory().getPath() + FILE_SEP + "1u" + FILE_SEP + "zip";
        File file = new File(s);
        file.mkdirs();
//        try {
//            String path = Environment.getExternalStorageDirectory().getPath() + FILE_SEP + "1u" + FILE_SEP + "zip" + FILE_SEP + "exception".hashCode();
        File file1 = new File(file, "exception".hashCode() + ".zip");
//            new File(path).createNewFile();
//            file1.createNewFile();
        return file1.getPath();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return s;
//        if (dir.endsWith(FILE_SEP)) {
//            return dir + "zip";
//        } else {
//            return dir + FILE_SEP + "zip";
//        }
    }

    //    public static void file(Object contents) {
//        log(FILE | D, sGlobalTag, contents);
//    }
//
//    public static void file(@TYPE int type, Object contents) {
//        log(FILE | type, sGlobalTag, contents);
//    }
//
//    public static void file(String tag, Object contents) {
//        log(FILE | D, tag, contents);
//    }
//
//    public static void file(@TYPE int type, String tag, Object contents) {
//        log(FILE | type, tag, contents);
//    }
//
//    public static void file(@TYPE int type, String tag, Object contents, Throwable ex) {
//        Writer writer = new StringWriter();
//        PrintWriter printWriter = new PrintWriter(writer);
//        ex.printStackTrace(printWriter);
//        String exceptionStr = writer.toString();
//
//        printWriter.close();
//        log(FILE | type, tag, contents + "\n" + exceptionStr);
//    }
//
//    public static void json(String contents) {
//        log(JSON | D, sGlobalTag, contents);
//    }
//
//    public static void json(@TYPE int type, String contents) {
//        log(JSON | type, sGlobalTag, contents);
//    }
//
//    public static void json(String tag, String contents) {
//        log(JSON | D, tag, contents);
//    }
//
//    public static void json(@TYPE int type, String tag, String contents) {
//        log(JSON | type, tag, contents);
//    }
//
//    public static void xml(String contents) {
//        log(XML | D, sGlobalTag, contents);
//    }
//
//    public static void xml(@TYPE int type, String contents) {
//        log(XML | type, sGlobalTag, contents);
//    }
//
//    public static void xml(String tag, String contents) {
//        log(XML | D, tag, contents);
//    }
//
//    public static void xml(@TYPE int type, String tag, String contents) {
//        log(XML | type, tag, contents);
//    }
//
    private static void log(final int type, String tag, final String... contents) {
        //设置的级别大于等于 打印的log type 才进行打印
        if (type >= level) {
            if (!sLogSwitch || (!sLog2ConsoleSwitch && !sLog2FileSwitch)) return;
            int type_low = type & 0x0f, type_high = type & 0xf0;
            if (type_low < sConsoleFilter && type_low < sFileFilter) return;
            final String[] tagAndHead = processTagAndHead(tag);
            String body = processBody(type_high, contents);
            if (sLog2ConsoleSwitch && type_low >= sConsoleFilter) {
                print2Console(type_low, tagAndHead[0], tagAndHead[1] + body);
            }
            if (sLog2FileSwitch) {
                if (type_low >= sFileFilter)
                    print2File(type_low, tagAndHead[0], tagAndHead[2] + body);
            }
        }
    }

    private static String[] processTagAndHead(String tag) {
        if (!sTagIsSpace && !sLogHeadSwitch) {
            tag = sGlobalTag;
        } else {
            StackTraceElement targetElement = Thread.currentThread().getStackTrace()[5];
            String className = targetElement.getClassName();
            String[] classNameInfo = className.split("\\.");
            if (classNameInfo.length > 0) {
                className = classNameInfo[classNameInfo.length - 1];
            }
            if (className.contains("$")) {
                className = className.split("\\$")[0];
            }
            if (sTagIsSpace) {
                tag = isSpace(tag) ? className : tag;
            }
            if (sLogHeadSwitch) {
                String head = new Formatter()
                        .format("%s, %s(%s.java:%d)",
                                Thread.currentThread().getName(),
                                targetElement.getMethodName(),
                                className,
                                targetElement.getLineNumber())
                        .toString();
                return new String[]{tag, head + LINE_SEP, " [" + head + "]: "};
            }
        }
        return new String[]{tag, "", ": "};
    }

    private static String processBody(int type, String... contents) {
        String body = NULL_TIPS;
        if (contents != null) {
            if (contents.length == 1) {
                Object object = contents[0];
                body = object == null ? NULL : object.toString();
//                if (type == JSON) {
//                    body = formatJson(body);
//                } else if (type == XML) {
//                    body = formatXml(body);
//                }
            } else {
                StringBuilder sb = new StringBuilder();
                for (int i = 0, len = contents.length; i < len; ++i) {
                    Object content = contents[i];
                    sb.append(ARGS)
                            .append("[")
                            .append(i)
                            .append("]")
                            .append(" = ")
                            .append(content == null ? NULL : content.toString())
                            .append(LINE_SEP);
                }
                body = sb.toString();
            }
        }
        return body;
    }
//
//    private static String formatJson(String json) {
//        try {
//            if (json.startsWith("{")) {
//                json = new JSONObject(json).toString(4);
//            } else if (json.startsWith("[")) {
//                json = new JSONArray(json).toString(4);
//            }
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//        return json;
//    }
//
//    private static String formatXml(String xml) {
//        try {
//            Source xmlInput = new StreamSource(new StringReader(xml));
//            StreamResult xmlOutput = new StreamResult(new StringWriter());
//            Transformer transformer = TransformerFactory.newInstance().newTransformer();
//            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
//            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
//            transformer.transform(xmlInput, xmlOutput);
//            xml = xmlOutput.getWriter().toString().replaceFirst(">", ">" + LINE_SEP);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return xml;
//    }

    private static void print2Console(final int type, String tag, String msg) {
        if (sLogBorderSwitch) {
            print(type, tag, TOP_BORDER);
            msg = addLeftBorder(msg);
        }
        int len = msg.length();
        int countOfSub = len / MAX_LEN;
        if (countOfSub > 0) {
            print(type, tag, msg.substring(0, MAX_LEN));
            String sub;
            int index = MAX_LEN;
            for (int i = 1; i < countOfSub; i++) {
                sub = msg.substring(index, index + MAX_LEN);
                print(type, tag, sLogBorderSwitch ? LEFT_BORDER + sub : sub);
                index += MAX_LEN;
            }
            sub = msg.substring(index, len);
            print(type, tag, sLogBorderSwitch ? LEFT_BORDER + sub : sub);
        } else {
            print(type, tag, msg);
        }
        if (sLogBorderSwitch) print(type, tag, BOTTOM_BORDER);
    }

    private static void print(final int type, final String tag, String msg) {
        Log.println(type, tag, msg);
    }

    private static String addLeftBorder(String msg) {
        if (!sLogBorderSwitch) return msg;
        StringBuilder sb = new StringBuilder();
        String[] lines = msg.split(LINE_SEP);
        for (String line : lines) {
            sb.append(LEFT_BORDER).append(line).append(LINE_SEP);
        }
        return sb.toString();
    }

    private static void print2File(final int type, final String tag, String msg) {
        Date now = new Date(System.currentTimeMillis());
        String format = FORMAT_FILE.format(now);
        String date = format.substring(0, 5);
        String time = format.substring(6);
        final String fullPath = (dir == null ? defaultDir : dir) + format + ".txt";
        if (!createOrExistsFile(fullPath)) {
            Log.e(tag, "log to " + fullPath + " failed!");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(FORMAT_CONTENT.format(now))
                .append(T[type - V])
                .append(String.format("[%d:%d] ", android.os.Process.myPid(), Thread.currentThread().getId()))
                .append(tag)
                .append(msg)
                .append(LINE_SEP);
        final String content = sb.toString();
        if (executor == null) {
            executor = Executors.newSingleThreadExecutor();
        }
        executor.execute(new Runnable() {
            @Override
            public void run() {
                BufferedWriter bw = null;
                try {
                    bw = new BufferedWriter(new FileWriter(fullPath, true));
                    bw.write(content);
                    Log.d(tag, "log to " + fullPath + " success!");
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(tag, "log to " + fullPath + " failed!");
                } finally {
                    try {
                        if (bw != null) {
                            bw.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private static boolean createOrExistsFile(String filePath) {
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

    private static boolean createOrExistsDir(File file) {
        return file != null && (file.exists() ? file.isDirectory() : file.mkdirs());
    }

    private static boolean isSpace(String s) {
        if (s == null) return true;
        for (int i = 0, len = s.length(); i < len; ++i) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
