package com.example.tpactivity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import static android.support.v4.content.PermissionChecker.checkSelfPermission;


public class TouchExample extends View {
    private float mScale = 4f;
    private ScaleGestureDetector mScaleGestureDetector; //scaleGestureDetector to detect zoom through pinch scale event
    private GestureDetector scrollGestureDetector; // gestureDetector to detect scroll events
    private Context context; // context of the application
    private Activity activity; // activity which uses this view
    private int offsetscroll=0; // value gotten from scroll events
    private int scale=4; // scale of the pictures (higher scale means smaller picture)
    private static final int MIN_SCALE = 1; //min scale of the pictures (biggest pictures)
    private static final int MAX_SCALE = 7; //max scale of the pictures (smallest pictures)
    private static ArrayList<String> listOfAllPicturesPaths = new ArrayList<>(); // list of pictures paths
    private static ArrayList<Bitmap> listOfAllBitmaps = new ArrayList<>(); // list of images bitmaps
    private int screenHeight;
    private int screenWidth;
    private static boolean cursorOnPicturesPathsInitialised = false;
    private static Cursor cursorOnPicturesPaths;

    /**
     * Constructor for the view
     * @param context The context of the application
     * @param activity The activity using this view
     */
    public TouchExample(Context context, final Activity activity) throws RuntimeException {
        super(context);

        /* saves the context and activity in a private variable */
        this.context=context;
        this.activity=activity;

        /* checks the activity's permissions */
        if(!isReadStoragePermissionGranted()){
            /* if we don't have read permission throw runtime exception, the activity won't work */
           throw new RuntimeException();
        }

        /* fills the list of images paths */
        getImagesPath(activity, 21);

        /* fills the list of bitmaps, created from the list of images paths */
        parseImage();

        /* defines our variable as a scaleGestureDectector */
        mScaleGestureDetector = new ScaleGestureDetector(context, new ScaleGesture());

        /* defines our variable as a gestureDectector with a listener */
        scrollGestureDetector = new GestureDetector(context, new ScrollGesture());
    }

    /**
     * Draws elements on the canvas to display them
     * @param canvas the canvas used to draw
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        /* defines the height of the screen */
        screenHeight = getHeight();

        /* position indexes for x and y */
        int posX=0,posY=0;

        /* the drawable to draw on the canvas */
        BitmapDrawable drawable;

        /* the width of the pictures: the width of the screen divided by the scale */
        screenWidth = getWidth();
        int pictureWidth=screenWidth/scale;

        /* for each bitmap, creates a bitmapDrawable, give its position and draws it */
        Iterator it = listOfAllBitmaps.iterator();

        while (it.hasNext()){
            drawable=new BitmapDrawable((Bitmap) it.next());
            /* offsetscroll allows us to scroll past the first row of pictures, and then back. offsetscroll is always positive */
            if(pictureWidth+posY-offsetscroll > 0)
            {
                drawable.setBounds(posX,posY-offsetscroll,posX+pictureWidth-1,pictureWidth+posY-offsetscroll-1);
            }

            /* increments the x position by the width of the pictures */
            posX+=pictureWidth;

            /* when we are at the end of a row (of the canvas's width), return to the beginning and increments the y position by the height of the pictures */
            if(posX+pictureWidth>getWidth()){
                posX=0;
                posY+=pictureWidth;
            }
            drawable.draw(canvas);
        }
    }

    /**
     * processes a touchEvent
     * @param event the motionEvent
     * @return true to allow other motionEvents after touchEvents (such as scale events)
     */

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        /* sets an onTouchEvent for our gestureDetector */
        scrollGestureDetector.onTouchEvent(event);

        /* sets an onTouchEvent for our scaleGestureDetector */
        mScaleGestureDetector.onTouchEvent(event);

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_CANCEL:
                invalidate();
                break;
        }
        return true;
    }

    /**
     * GestureListener to detect onScroll events and process them
     */
    public class ScrollGesture extends GestureDetector.SimpleOnGestureListener {
        /* defines the behavior for a scroll event */
        @Override
        public boolean onScroll(final MotionEvent e1, final MotionEvent e2, final float distanceX, final float distanceY) {
            int maxScrollableHeight = Math.max(0, -screenHeight + (int) (((double) screenWidth / scale) * (int) (Math.ceil((double) listOfAllBitmaps.size() / scale))));
            /* if the scroll is made within the gallery bounds, keeps the scrolled distance in the offsetscroll variable */
            if (offsetscroll + distanceY > 0 && offsetscroll + distanceY < maxScrollableHeight) {
                offsetscroll += distanceY;
            }
            /* if the operation would make us scroll higher than the pictures, sets offsetscroll to 0 */
            else if (offsetscroll + distanceY <= 0) {
                offsetscroll = 0;
            }
            /* if the operation would make us scroll lower than the pictures, sets offsetscroll to the maxscrollableheight (the bottom line for the last pictures */
            else {
                offsetscroll = maxScrollableHeight;
                getImagesPath(activity, 7);
                parseImage();
            }

            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }
    }

    /**
     * ScaleListener to detect onScale events and use them
     */
    public class ScaleGesture extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScale = mScale/(detector.getScaleFactor()*detector.getScaleFactor());
            mScale = Math.max(MIN_SCALE, Math.min(mScale, MAX_SCALE)); //ensure scale is between min and max
            scale = (int) mScale; //rounds scale in an int
            invalidate();
            return true;
        }
    }

    /**
     * Gets the paths for numberToLoad not yet loaded images on the device and stores them in a global list of strings.
     * @param activity the activity using this view
     * @param numberToLoad the number of images to get from the device
     */
    public static void getImagesPath(Activity activity, int numberToLoad) {
        /* Uri to access external content */
        Uri uri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = { MediaStore.MediaColumns.DATA };

        /* path of the current image */
        String ImagePath;
        if(!cursorOnPicturesPathsInitialised){
            cursorOnPicturesPathsInitialised = true;
            cursorOnPicturesPaths = activity.getContentResolver().query(uri, projection, null,
                    null, null);
        }

        int column_index_data;
        if (cursorOnPicturesPaths != null) {
            column_index_data = cursorOnPicturesPaths.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);

            int numberOfPicuresAdded=0; //number of pictures gotten from the device

            while (cursorOnPicturesPaths.moveToNext() && numberOfPicuresAdded < numberToLoad) {
                /* the current image's path is stored in cursor */
                ImagePath = cursorOnPicturesPaths.getString(column_index_data);

                /* add the current image's path to the list of all images' paths */
                listOfAllPicturesPaths.add(ImagePath);

                /* increase the count of pictures loaded */
                numberOfPicuresAdded++;
            }
        }
    }

    /**
     * Reads the list of images' paths and makes a bitmap for each one, then crops it in a square and stores it in a global list of bitmaps
     */
    public void parseImage()
    {
        Bitmap myBitmap;
        Bitmap croppedBitmap;
        /* for each image path, creates a new file from the path and makes a bitmap out of it */
        for(int i = listOfAllBitmaps.size(); i < listOfAllPicturesPaths.size(); i++){
            File imgFile = new File(listOfAllPicturesPaths.get(i));
            if(imgFile.exists()){
                /* scales down the image size to avoid OutOfMemory Exceptions */
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 4;

                /* makes a bitmap from the file created before */
                myBitmap=BitmapFactory.decodeFile(imgFile.getAbsolutePath(),options);

                /* gets the size of the smallest side */
                int size = myBitmap.getWidth()<myBitmap.getHeight()? myBitmap.getWidth() : myBitmap.getHeight();

                /* creates a new bitmap from the first, cropped in a square */
                croppedBitmap =Bitmap.createBitmap(myBitmap, 0,0,size,size);

                /* adds the bitmap to the list of bitmaps */
                listOfAllBitmaps.add(croppedBitmap);
            }
        }
    }

    /**
     * Checks if the permission to read external storage is granted to the activity
     * Necessary for Android versions >= 23. For the others, permission declaration in the manifest file is enough.
     * @return if the permission is granted or not
     */

    public boolean isReadStoragePermissionGranted(){
        /* If we need to check the permission, check it. Otherwise the application automatically has it */
        if (Build.VERSION.SDK_INT >= 23){
            if(checkSelfPermission(context,Manifest.permission.READ_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED){
                return true;
            }
            else{
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},3);
                return false;
            }
        }
        else{
            return true;
        }
    }
}
