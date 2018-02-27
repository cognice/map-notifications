package us.cognice.secrets.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import us.cognice.secrets.data.Location;

import java.io.*;
import java.util.Locale;

import static android.content.Context.INPUT_METHOD_SERVICE;

/**
 * Created by Kirill Simonov on 07.10.2017.
 */
public class Utils {

    public static float dipToPixels(Context context, float dipValue) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics);
    }

    public static void hideKeyboard(View view) {
        InputMethodManager manager = (InputMethodManager) view.getContext().getSystemService(INPUT_METHOD_SERVICE);
        if (manager != null) manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static String formatMeters(int meters) {
        return meters < 1000 ? meters + " m" : String.format(Locale.ROOT, "%.1f", (double) meters / 1000) + " km";
    }

    public static String formatCoordinate(double coordinate) {
        return String.format(Locale.ROOT, "%.3f", coordinate);
    }

    public static Bitmap getBitmap(Context context, int id) {
        Drawable drawable = ContextCompat.getDrawable(context, id);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            drawable = (DrawableCompat.wrap(drawable)).mutate();
        }
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static void copy(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src)) {
            try (OutputStream out = new FileOutputStream(dst)) {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }
    }

    public static void copyLocation(Location from, Location to) {
        to.setLongitude(from.getLongitude());
        to.setLatitude(from.getLatitude());
        to.setActive(from.isActive());
        to.setMessage(from.getMessage());
        to.setRadius(from.getRadius());
        to.setName(from.getName());
    }

}
