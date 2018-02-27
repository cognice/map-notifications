package us.cognice.secrets.drawable;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import us.cognice.secrets.fragments.ManageFragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Kirill Simonov on 11.10.2017.
 */
public class AvatarTarget implements Target {

    private final File file;
    private final ManageFragment parent;

    public AvatarTarget(File file, ManageFragment parent) {
        this.file = file;
        this.parent = parent;
    }

    @Override
    public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {
        new SaveBitmapTask().execute(bitmap);
    }

    @Override
    public void onBitmapFailed(Drawable errorDrawable) {
        Log.e("Failed to save avatar: ", errorDrawable.toString());
    }

    @Override
    public void onPrepareLoad(Drawable placeHolderDrawable) {
        Log.i("Avatar", "Saving " + file.getAbsolutePath());
    }

    private class SaveBitmapTask extends AsyncTask<Bitmap, Integer, File> {

        @Override
        protected File doInBackground(Bitmap... files) {
            try {
                FileOutputStream out = new FileOutputStream(file, false);
                files[0].compress(Bitmap.CompressFormat.PNG, 100, out);
                out.flush();
                out.close();
            } catch (IOException e) {
                Log.e("Failed to save avatar: ", e.getLocalizedMessage());
            }
            return file;
        }

        @Override
        protected void onPostExecute(File file) {
            super.onPostExecute(file);
            //parent.setAvatar(file);
        }
    }
}
