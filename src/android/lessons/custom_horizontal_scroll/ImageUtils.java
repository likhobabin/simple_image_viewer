package android.lessons.custom_horizontal_scroll;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.MailTo;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Ilya <ilya.likhobabin@gmail.com>
 */
class ImageUtils {

	public static int IMG_MATRIX_SIZE = 9;

	public static int IMG_INDEX_MASK = 0x10;

	public static final String[] IMG_URL_ARRAY = {
		"http://treto.ru/img_lb/Settecento/.IT/per_sito/ambienti/IT-.jpg",
		"http://treto.ru/img_lb/Settecento/.IT/per_sito/ambienti/IT-2.jpg",
		"http://treto.ru/img_lb/Settecento/.IT/per_sito/ambienti/IT-4.jpg",
		"http://treto.ru/img_lb/Settecento/.IT/per_sito/ambienti/IT-5.jpg",
		"http://treto.ru/img_lb/Settecento/.IT/per_sito/ambienti/IT-6.jpg"
	};

	/**
	 * Calculates a distance between two fingers.
	 *
	 * @param ff the first finger
	 * @param sf the second finger
	 * @return result distance
	 * @throws IllegalArgumentException throws if arguments are null references
	 */
	public static float calcDistance(PointF ff, PointF sf) throws IllegalArgumentException {
		checkNullArguments(ff, sf);

		float dx = ff.x - sf.x;
		float dy = ff.y - sf.y;
		return FloatMath.sqrt(dx * dx + dy * dy);
	}

	/**
	 * Calculate a middle point between two fingers based on their coordinates. Result is set into
	 * the passed {@link android.graphics.PointF} object.
	 *
	 * @param ff       the first finger
	 * @param sf       the second finger
	 * @param midPoint the result mid point
	 * @throws IllegalArgumentException throws if arguments are null references
	 */
	public static void calculateMidPoint(PointF ff, PointF sf, PointF midPoint)
		throws IllegalArgumentException {

		checkNullArguments(midPoint, ff, sf);
		midPoint.set((ff.x + sf.x) / 2, (ff.y + sf.y) / 2);
	}

	public static void scaleImage(ImageView imView, int screenWidth, int screenHeight)
		throws IllegalArgumentException {

		checkNullArguments(imView);

		Drawable temp = imView.getDrawable();
		Bitmap imBitmap = ((BitmapDrawable) temp).getBitmap();
		int imWidth = imBitmap.getWidth();
		int imHeight = imBitmap.getHeight();
		float xScale = ((float) screenWidth) / imWidth;
		float yScale = ((float) screenHeight) / imHeight;
		float scale = xScale <= yScale ? xScale : yScale;
		Matrix scaleMatrix = new Matrix();

		scaleMatrix.postScale(scale, scale);

		Bitmap scBitmap = Bitmap.createBitmap(imBitmap, 0, 0, imWidth, imHeight, scaleMatrix, true);

		BitmapDrawable scDrBitmap = new BitmapDrawable(imView.getResources(), scBitmap);
		imView.setImageDrawable(scDrBitmap);
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
			scBitmap.getWidth(), scBitmap.getHeight()
		);

		layoutParams.gravity = Gravity.FILL_HORIZONTAL | Gravity.CENTER_VERTICAL;

		if (scBitmap.getWidth() < screenWidth) {
			int margin = (screenWidth - scBitmap.getWidth()) / 2;
			layoutParams.leftMargin = margin;
			layoutParams.rightMargin = margin;
		}

		if (scBitmap.getHeight() < screenHeight) {
			int margin = (screenHeight - scBitmap.getHeight()) / 2;
			layoutParams.topMargin = margin;
			layoutParams.bottomMargin = margin;
		}

		Log.d("IU_SCALE", "Bitmap width: " + scBitmap.getWidth() + " height: " + scBitmap.getHeight() +
			" Screen width " + screenWidth + " Screen height " + screenHeight);

		imView.setLayoutParams(layoutParams);
	}

	public static ImageView createScaledImageByBitmap(Context context, Bitmap srcBitmap, int screenWidth, int screenHeight) {
		checkNullArguments(context, srcBitmap);

		ImageView resImg = new ImageView(context);
		BitmapDrawable scDrBitmap = new BitmapDrawable(null, srcBitmap);

		resImg.setScaleType(ImageView.ScaleType.MATRIX);
		resImg.setImageDrawable(scDrBitmap);
		scaleImage(resImg, screenWidth, screenHeight);

		return resImg;
	}

	public static int[] getDisplayMetrics(Activity anActivity) throws IllegalArgumentException {
		checkNullArguments(anActivity);

		DisplayMetrics metrics = new DisplayMetrics();
		anActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

		return new int[]{metrics.widthPixels, metrics.heightPixels};
	}

	public static int[] createDimension() {
		return new int[2];
	}

	public static void getImageSize(ImageView imageView, int[] dst) throws IllegalArgumentException {
		checkNullArguments(imageView, dst);

		Bitmap imBitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();

		dst[0] = imBitmap.getWidth();
		dst[1] = imBitmap.getHeight();
	}

	public static float getTransformationX(float srcX, float[] matrix) throws IllegalArgumentException {
		checkNullArguments(srcX, matrix);
		if (matrix.length != IMG_MATRIX_SIZE) {
			throw new IllegalArgumentException("Wrong size of the transformation image matrix");
		}

		return srcX * matrix[0] + matrix[2];
	}

	public static float getTransformationY(float srcY, float[] matrix) throws IllegalArgumentException {
		checkNullArguments(srcY, matrix);
		if (matrix.length != IMG_MATRIX_SIZE) {
			throw new IllegalArgumentException("Wrong size of the transformation image matrix");
		}

		return srcY * matrix[4] + matrix[5];
	}

	public static void checkNullArguments(Object... args) throws IllegalArgumentException {
		for (Object arg : args) {
			if (arg == null) {
				throw new IllegalArgumentException("Invalid arguments");
			}
		}
	}

	public static InputStream openImageInputStream(String httpPath) throws MalformedURLException, IOException,
		IllegalArgumentException {
		checkNullArguments(httpPath);
		URL url = new URL(httpPath);
		URLConnection urlConnection = url.openConnection();
		HttpURLConnection conn = (HttpURLConnection) urlConnection;

		int respCode = conn.getResponseCode();
		if (respCode == HttpURLConnection.HTTP_OK) {
			return conn.getInputStream();
		} else {
			throw new IllegalArgumentException("Can't load image");
		}

	}

	private ImageUtils() {
		// NOP
	}

}
