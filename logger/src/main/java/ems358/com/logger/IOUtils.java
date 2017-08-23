package ems358.com.logger;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by hcqi on.
 * Des:
 * Date: 2017/8/2
 */

public class IOUtils {
    public static void closeIO(final Closeable... closeables) {
        if (closeables == null) return;
        for (Closeable closeable : closeables) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
