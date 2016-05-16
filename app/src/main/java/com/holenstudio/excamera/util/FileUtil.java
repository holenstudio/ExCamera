package com.holenstudio.excamera.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.JSONArray;

public class FileUtil {
	private static final String TAG = "FileUtil";

	public static String loadAssertFile(Context cxt, String fileName) throws IOException {
        InputStream is = cxt.getAssets().open(fileName);
        byte[] buffer = new byte[is.available()];
        is.read(buffer);
        is.close();

        return new String(buffer);
    }

    public static File rootDir() {
        File root = new File(Environment.getExternalStorageDirectory(), "ExCamera");
        if (!root.exists()) {
            root.mkdir();
        }
        return root;
    }

    public static File dirWithName(String name) {
        File dir = new File(rootDir(), name);
        if (!dir.exists()) {
            dir.mkdir();
        }
        return dir;
    }

    public static File photoDir() {
        return dirWithName("photo");
    }
    
    public static File videoDir() {
        return dirWithName("video");
    }

    public static String getImageFileName() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
        return "photo-" + formatter.format(new Date()) + ".jpg";
    }

    public static String savePhoto(Bitmap bmp) {
        return  savePhoto(bmp, getImageFileName());
    }

    public static String savePhoto(Bitmap bmp, String fileName) {
        try {
            File file = new File(photoDir(), fileName);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(file));
            return file.getAbsolutePath();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static String savePhoto(byte[] data, String fileName) {
    	return saveFile(data, photoDir(), fileName);
    }
    
    public static String saveFile(byte[] data, File path, String fileName) {
    	File file = new File(path, fileName);
    	try {
			FileOutputStream fos = new FileOutputStream(file);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			bos.write(data);
			bos.close();
			fos.close();
			return file.getAbsolutePath();
		} catch (IOException e) {
			Log.d(TAG, "File not found:" + e.getMessage());
		}
    	return null;
    }

    public static String getRealFilePath(Context ctx, Uri uri) {
        if (null == uri) {
            return null;
        }

        String scheme = uri.getScheme();
        String path = null;
        if (scheme == null) {

            path = uri.getPath();
        } else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            path = uri.getPath();
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            Cursor cursor = ctx.getContentResolver().query(uri, new String[] {
                    MediaStore.Images.ImageColumns.DATA
            }, null, null, null);

            if (null != cursor) {
                if (cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                    if (index > -1) {
                        path = cursor.getString(index);
                    }
                }
                cursor.close();
            }
        }
        return path;
    }

    public static String GetImageStr(String fileName) throws IOException {
        File file = new File(fileName);
        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[fis.available()];
        fis.read(buffer);
        fis.close();

        return Base64.encodeToString(buffer, Base64.DEFAULT);

    }

    public static String GetImage(String fileName, String fileString) throws IOException {
        File file = new File(photoDir(), fileName);
        FileOutputStream fos = new FileOutputStream(file);
        byte[] buffer = Base64.decode(fileString, Base64.DEFAULT);
        fos.write(buffer);
        fos.close();

        return file.getAbsolutePath();
    }

}
