package io.rong.imkit.plugin.image;


import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import androidx.annotation.RequiresApi;
import androidx.collection.LruCache;
import android.util.Log;
import android.view.WindowManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.rong.common.FileUtils;
import io.rong.common.RLog;
import io.rong.imkit.utilities.RongUtils;
import io.rong.imkit.utilities.KitStorageUtils;

import static android.content.Context.ACTIVITY_SERVICE;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.HONEYCOMB_MR1;
import static android.os.Build.VERSION_CODES.KITKAT;

public class AlbumBitmapCacheHelper {
    private final static String TAG = "AlbumBitmapCacheHelper";
    //线程安全的单例模式
    private volatile static AlbumBitmapCacheHelper instance = null;
    private LruCache<String, Bitmap> cache;
    private static int cacheSize = 1024 * 1024;
    /**
     * 用来优化图片的展示效果，保存当前显示的图片path
     */
    private ArrayList<String> currentShowString;

    private AlbumBitmapCacheHelper() {
        //分配1/4的运行时内存给图片显示
        //final int memory = (int) (Runtime.getRuntime().maxMemory() /  4);

        cache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                //获取每张bitmap大小
                //return value.getRowBytes() * value.getHeight() / 1024;
                int result;
                if (SDK_INT >= KITKAT) {
                    result = value.getAllocationByteCount();
                } else if (SDK_INT >= HONEYCOMB_MR1) {
                    result = value.getByteCount();
                } else {
                    result = value.getRowBytes() * value.getHeight();
                }
                return result;
            }
        };

        currentShowString = new ArrayList<String>();
    }

    /**
     * 释放所有的内存
     */
    public void releaseAllSizeCache() {
        cache.evictAll();
        cache.resize(1);
    }

    public void releaseHalfSizeCache() {
        cache.resize((int) (Runtime.getRuntime().maxMemory() / 1024 / 8));
    }

    public void resizeCache() {
        cache.resize((int) (Runtime.getRuntime().maxMemory() / 1024 / 4));
    }

    /**
     * 选择完毕，直接释放缓存所占的内存
     */
    private void clearCache() {
        cache.evictAll();
        cache = null;
        tpe = null;
        instance = null;
    }

    public static AlbumBitmapCacheHelper getInstance() {
        if (instance == null) {
            synchronized (AlbumBitmapCacheHelper.class) {
                if (instance == null) {
                    instance = new AlbumBitmapCacheHelper();
                }
            }
        }
        return instance;
    }

    private Context mContext;

    public static void init(Context context) {
        RLog.d(TAG, "init");
        cacheSize = calculateMemoryCacheSize(context);
        AlbumBitmapCacheHelper helper = getInstance();
        helper.mContext = context.getApplicationContext();
    }

    public void uninit() {
        RLog.d(TAG, "uninit");
        tpe.shutdownNow();
        clearCache();
    }

    /**
     * 通过图片的path回调该图片的bitmap
     *
     * @param path     图片地址
     * @param width    需要显示图片的宽度，0代表显示完整图片
     * @param height   需要显示图片的高度，0代表显示完整图片
     * @param callback 加载bitmap成功回调
     * @param objects  用来直接返回标识
     */
    public Bitmap getBitmap(final String path, int width, int height, final ILoadImageCallback callback, Object... objects) {
        Bitmap bitmap = getBitmapFromCache(path, width, height);
        //如果能够从缓存中获取符合要求的图片，则直接回调
        if (bitmap != null) {
            Log.e(TAG, "getBitmap from cache");
        } else {
            decodeBitmapFromPath(path, width, height, callback, objects);
        }
        return bitmap;
    }

    //try another size to get better display
    ThreadPoolExecutor tpe = new ThreadPoolExecutor(2, 5, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    /**
     * 通过path获取图片bitmap
     */
    private void decodeBitmapFromPath(final String path, final int width, final int height, final ILoadImageCallback callback, final Object... objects) throws OutOfMemoryError {
        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (callback != null)
                    callback.onLoadImageCallBack((Bitmap) msg.obj, path, objects);
            }
        };
        //防止主线程卡顿
        tpe.execute(new Runnable() {
            @Override
            public void run() {
                if (!currentShowString.contains(path) || cache == null) {
                    return;
                }
                Bitmap bitmap = null;
                //返回大图,屏幕宽度为准
                if (width == 0 || height == 0) {
                    try {
                        bitmap = getBitmap(path, width, height);
                    } catch (OutOfMemoryError e) {
                        RLog.e(TAG, "decodeBitmapFromPath", e);
                    }
                } else {
                    //返回小图，第一步，从temp目录下取该图片指定大小的缓存，如果取不到，
                    // 第二步，计算samplesize,如果samplesize > 4,
                    // 第三步则将压缩后的图片存入temp目录下，以便下次快速取出
                    String hash = RongUtils.md5(path + "_" + width + "_" + height);
                    //临时文件的文件名
                    String tempPath = FileUtils.getInternalCachePath(mContext, "image") + "/" + hash + ".temp";
                    File picFile = new File(path);
                    File tempFile = new File(tempPath);
                    //如果该文件存在,并且temp文件的创建时间要原文件之后
                    if (tempFile.exists() && (picFile.lastModified() <= tempFile.lastModified()))
                        bitmap = BitmapFactory.decodeFile(tempPath);
                    //无法在临时文件的缩略图目录找到该图片，于是执行第二步
                    if (bitmap == null) {
                        try {
                            bitmap = getBitmap(path, width, height);
                        } catch (OutOfMemoryError e) {
                            bitmap = null;
                        }
                        if (bitmap != null && cache != null) {
                            bitmap = centerSquareScaleBitmap(bitmap, ((bitmap.getWidth() > bitmap.getHeight()) ? bitmap.getHeight() : bitmap.getWidth()));
                        }
                        if (bitmap != null) {
                            FileOutputStream fos = null;
                            try {
                                File file = new File(tempPath);
                                if (!file.exists())
                                    file.createNewFile();
                                else {
                                    file.delete();
                                    file.createNewFile();
                                }
                                fos = new FileOutputStream(file);
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                                fos.write(baos.toByteArray());
                                fos.flush();
                            } catch (FileNotFoundException e) {
                                RLog.e(TAG, "decodeBitmapFromPath", e);
                            } catch (IOException e) {
                                RLog.e(TAG, "decodeBitmapFromPath", e);
                            } finally {
                                if (fos != null) {
                                    try {
                                        fos.close();
                                    } catch (IOException e) {
                                        RLog.e(TAG, "decodeBitmapFromPath", e);
                                    }
                                }
                            }
                        }
                    } else {
                        //从temp目录加载出来的图片也要放入到cache中
                        if (cache != null) {
                            bitmap = centerSquareScaleBitmap(bitmap, ((bitmap.getWidth() > bitmap.getHeight()) ? bitmap.getHeight() : bitmap.getWidth()));
                        }
                    }
                }
                if (bitmap != null && cache != null)
                    cache.put(path + "_" + width + "_" + height, bitmap);
                Message msg = Message.obtain();
                msg.obj = bitmap;
                handler.sendMessage(msg);
            }
        });
    }

    /**
     * @param bitmap     原图
     * @param edgeLength 希望得到的正方形部分的边长
     * @return 缩放截取正中部分后的位图。
     */
    public static Bitmap centerSquareScaleBitmap(Bitmap bitmap, int edgeLength) {
        if (null == bitmap || edgeLength <= 0) {
            return null;
        }
        Bitmap result = bitmap;
        int widthOrg = bitmap.getWidth();
        int heightOrg = bitmap.getHeight();

        //从图中截取正中间的正方形部分。
        int xTopLeft = (widthOrg - edgeLength) / 2;
        int yTopLeft = (heightOrg - edgeLength) / 2;

        if (xTopLeft == 0 && yTopLeft == 0) return result;

        try {
            result = Bitmap.createBitmap(bitmap, xTopLeft, yTopLeft, edgeLength, edgeLength);
            if (!bitmap.isRecycled())
                bitmap.recycle();
        } catch (OutOfMemoryError e) {
            return result;
        }

        return result;
    }

    /**
     * 计算缩放比例
     */
    private int computeScale(BitmapFactory.Options options, int width, int height) {
        if (options == null) return 1;
        int widthScale = (int) ((float) options.outWidth / (float) width);
        int heightScale = (int) ((float) options.outHeight / (float) height);
        //选择缩放比例较大的那个
        int scale = (widthScale > heightScale ? widthScale : heightScale);
        if (scale < 1) scale = 1;
        return scale;
    }

    /**
     * 获取lrucache中的图片，如果该图片的宽度和长度无法和需要的相符，则返回null
     *
     * @param path   图片地址,key
     * @param width  需要的图片宽度
     * @param height 需要的图片长度
     * @return 图片value
     */
    private Bitmap getBitmapFromCache(final String path, int width, int height) {
        return cache.get(path + "_" + width + "_" + height);
    }

    /**
     * 将要展示的path加入到list
     */
    public void addPathToShowlist(String path) {
        currentShowString.add(path);
    }

    /**
     * 从展示list中删除该path
     */
    public void removePathFromShowlist(String path) {
        currentShowString.remove(path);
    }

    /**
     * 加载图片成功的接口回调
     */
    public interface ILoadImageCallback {
        void onLoadImageCallBack(Bitmap bitmap, String path, Object... objects);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private Bitmap getBitmap(String path, int widthLimit, int heightLimit) throws OutOfMemoryError {
        Bitmap bitmap = null;

        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            ExifInterface exifInterface = null;
            //Android Q系统适配
            if (KitStorageUtils.isBuildAndTargetForQ(mContext)) {
                //路径判断
                if (!FileUtils.uriStartWithContent(Uri.parse(path))) {
                    //如果是包名下文件路径，直接使用path地址获取exif
                    exifInterface = new ExifInterface(path);
                } else {
                    //获取图片exif信息时，需要使用FD来获取，原有通过path路径获取受限。
                    ParcelFileDescriptor pfd = mContext.getContentResolver().openFileDescriptor(Uri.parse(path), "r");
                    exifInterface = new ExifInterface(pfd.getFileDescriptor());
                }
            } else {
                exifInterface = new ExifInterface(path);
            }
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0);
            int sampleSize;
            if (widthLimit == 0 && heightLimit == 0) {
                sampleSize = computeScale(options, ((WindowManager) (mContext.getSystemService(Context.WINDOW_SERVICE))).getDefaultDisplay().getWidth(),
                        ((WindowManager) (mContext.getSystemService(Context.WINDOW_SERVICE))).getDefaultDisplay().getHeight());
                RLog.d(TAG, "sampleSize:" + sampleSize);
            } else {
                if (orientation == ExifInterface.ORIENTATION_ROTATE_90
                        || orientation == ExifInterface.ORIENTATION_ROTATE_270
                        || orientation == ExifInterface.ORIENTATION_TRANSPOSE
                        || orientation == ExifInterface.ORIENTATION_TRANSVERSE) {
                    int tmp = widthLimit;
                    widthLimit = heightLimit;
                    heightLimit = tmp;
                }

                sampleSize = computeScale(options, widthLimit, heightLimit);
                RLog.d(TAG, "sampleSize:" + sampleSize);
            }
            try {
                options = new BitmapFactory.Options();
                options.inJustDecodeBounds = false;
                options.inSampleSize = sampleSize;
                if (KitStorageUtils.isBuildAndTargetForQ(mContext)) {
                    //路径判断
                    if (!FileUtils.uriStartWithContent(Uri.parse(path))) {
                        //如果是包名下文件路径，直接使用path地址获取bitmap
                        bitmap = BitmapFactory.decodeFile(path, options);
                    } else {
                        //获取bitmap时，需要使用FD来获取，原有通过path路径获取受限。
                        ParcelFileDescriptor pfd = mContext.getContentResolver().openFileDescriptor(Uri.parse(path), "r");
                        bitmap = BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor(), null, options);
                    }
                } else {
                    bitmap = BitmapFactory.decodeFile(path, options);
                }
            } catch (OutOfMemoryError e) {
                RLog.e(TAG, "getBitmap", e);
                options.inSampleSize = options.inSampleSize << 1;
                if (KitStorageUtils.isBuildAndTargetForQ(mContext)) {
                    //路径判断
                    String imageDirPath = KitStorageUtils.getImageSavePath(mContext);
                    if (path.startsWith(imageDirPath)) {
                        //如果是包名下文件路径，直接使用path地址获取bitmap
                        bitmap = BitmapFactory.decodeFile(path, options);
                    } else {
                        //获取bitmap时，需要使用FD来获取，原有通过path路径获取受限。
                        ParcelFileDescriptor pfd = mContext.getContentResolver().openFileDescriptor(Uri.parse(path), "r");
                        bitmap = BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor(), null, options);
                    }
                } else {
                    bitmap = BitmapFactory.decodeFile(path, options);
                }
            }
            Matrix matrix = new Matrix();
            if (bitmap != null) {
                int w = bitmap.getWidth();
                int h = bitmap.getHeight();

                if (orientation == ExifInterface.ORIENTATION_ROTATE_90
                        || orientation == ExifInterface.ORIENTATION_ROTATE_270
                        || orientation == ExifInterface.ORIENTATION_TRANSPOSE
                        || orientation == ExifInterface.ORIENTATION_TRANSVERSE) {
                    int tmp = w;
                    w = h;
                    h = tmp;
                }
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        matrix.setRotate(90, w / 2f, h / 2f);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        matrix.setRotate(180, w / 2f, h / 2f);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        matrix.setRotate(270, w / 2f, h / 2f);
                        break;
                    case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                        matrix.preScale(-1, 1);
                        break;
                    case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                        matrix.preScale(1, -1);
                        break;
                    case ExifInterface.ORIENTATION_TRANSPOSE:
                        matrix.setRotate(90, w / 2f, h / 2f);
                        matrix.preScale(1, -1);
                        break;
                    case ExifInterface.ORIENTATION_TRANSVERSE:
                        matrix.setRotate(270, w / 2f, h / 2f);
                        matrix.preScale(1, -1);
                        break;
                }
                if (widthLimit == 0 || heightLimit == 0) {
                    RLog.e(TAG, "widthLimit or heightLimit is 0");
                } else {
                    float xS = (float) widthLimit / bitmap.getWidth();
                    float yS = (float) heightLimit / bitmap.getHeight();
                    matrix.postScale(Math.min(xS, yS), Math.min(xS, yS));
                }
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }
        } catch (IOException e) {
            RLog.e(TAG, "getBitmap", e);
        } catch (IllegalArgumentException e) {
            RLog.e(TAG, "getBitmap", e);
        }

        return bitmap;
    }


    private static int calculateMemoryCacheSize(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        boolean largeHeap = (context.getApplicationInfo().flags & (1 << 20)) != 0;
        int memoryClass = am.getMemoryClass();
        if (largeHeap) {
            memoryClass = am.getLargeMemoryClass();
        }
        // Target ~15% of the available heap.
        return (int) (1024L * 1024L * memoryClass / 8);
    }
}
