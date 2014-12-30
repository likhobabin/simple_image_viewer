package android.lessons.custom_horizontal_scroll;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

import static android.lessons.custom_horizontal_scroll.ImageUtils.IMG_INDEX_MASK;
import static android.lessons.custom_horizontal_scroll.ImageUtils.IMG_URL_ARRAY;

/**
 * Ilya <ilya.likhobabin@gmail.com>
 */
public class ImageLoaderTask extends AsyncTask<String, String, String> {

	private final ImageHorizontalScrollView hsv;

	private final LinearLayout horizScrollViewLayout;

	private ImageView loadImg;

	private Bitmap loadImgBitmap;

	public ImageLoaderTask(ImageHorizontalScrollView hsv, LinearLayout horizScrollViewLayout) {
		this.hsv = hsv;
		this.horizScrollViewLayout = horizScrollViewLayout;
	}

	@Override
	protected void onPreExecute() {
		hsv.getPb().setProgress(0);
	}

	@Override
	protected String doInBackground(String... params) {
		InputStream iis = null;
		try {
			long startTime = System.currentTimeMillis();

			iis = ImageUtils.openImageInputStream(IMG_URL_ARRAY[hsv.getImageIndex()]);
			loadImgBitmap = BitmapFactory.decodeStream(iis);

			long endTime = System.currentTimeMillis();
			Log.d("ImageLoaderTask", "Lost time (ms) " + (endTime - startTime));

			int bytes = loadImgBitmap.getByteCount();
			double oneMsByte = (double) (endTime - startTime) / (double) bytes;
			int onePercentBytes = bytes / 100;
			double onePercentTime = oneMsByte * onePercentBytes;
			int count = 0;
			while (count <= bytes) {
				count += onePercentBytes;
				try {
					Thread.sleep((long)onePercentTime);
				} catch (InterruptedException ex) {
					Log.e("ImageLoaderTask", "Error progress calculation");
				}
				publishProgress(String.valueOf(count/onePercentBytes));
			}

		} catch (MalformedURLException ex) {
			Log.e("ImageLoaderTask", "Image loading error " + ex.getLocalizedMessage());
		} catch (IOException ex) {
			Log.e("ImageLoaderTask", "Image loading error " + ex.getLocalizedMessage());

		} catch (RuntimeException ex) {
			Log.e("ImageLoaderTask", "Image loading error " + ex.getLocalizedMessage());
		} finally {
			try {
				if (iis != null) {
					iis.close();
				}
			} catch (IOException ex) {
				Log.e("ImageLoaderTask", "Can't close input stream of the image" + ex.getLocalizedMessage());
			}
		}

		return "";
	}

	@Override
	protected void onProgressUpdate(String... progressValues) {
		hsv.getPb().setProgress(Integer.valueOf(progressValues[0]));
	}

	@Override
	protected void onPostExecute(String result) {
		if (loadImgBitmap != null) {
			loadImg = ImageUtils.createScaledImageByBitmap(hsv.getContext(), loadImgBitmap, hsv.getDisplayMetrics()[0],
				hsv.getDisplayMetrics()[1]);

			loadImg.setId(hsv.getImageIndex() ^ IMG_INDEX_MASK);
		}
		if (loadImg == null) {
			int imgId = hsv.getImageIndex() ^ IMG_INDEX_MASK;

			loadImg = new ImageView(hsv.getContext());
			loadImg.setId(imgId);
			loadImg.setImageResource(R.drawable.img_404);
			loadImg.setScaleType(ImageView.ScaleType.MATRIX);
			ImageUtils.scaleImage(loadImg, hsv.getDisplayMetrics()[0], hsv.getDisplayMetrics()[1]);
		}
		hsv.getImageItems()[hsv.getImageIndex()].isLoad = true;
		loadImg.setImageMatrix(hsv.getImageItems()[hsv.getImageIndex()].getMatrix());

		horizScrollViewLayout.removeView(hsv.getPbl());
		horizScrollViewLayout.addView(loadImg);

		hsv.updateImageBounds();
		hsv.addImageSize(loadImg);
	}

}

