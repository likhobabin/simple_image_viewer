package android.lessons.custom_horizontal_scroll;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

import static android.lessons.custom_horizontal_scroll.ImageUtils.*;

/**
 * Ilya <ilya.likhobabin@gmail.com>
 */
public class ImageLoaderTask extends AsyncTask<Integer, String, String> {

	private final ImageHorizontalScrollView hsv;

	private final LinearLayout innerLayout;

	private View pbl;

	private ImageView loadImg;

	private Bitmap loadImgBitmap;

	public ImageLoaderTask(ImageHorizontalScrollView hsv, LinearLayout innerLayout) {
		this.hsv = hsv;
		this.innerLayout = innerLayout;
	}

	@Override
	protected void onPreExecute() {
	}

	@Override
	protected String doInBackground(Integer... params) {
		InputStream iis = null;
		try {
			iis = ImageUtils.loadImage(IMG_URL_ARRAY[hsv.getImageIndex()]);
			loadImgBitmap = BitmapFactory.decodeStream(iis);
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

		try {
			Thread.sleep(1000);
		} catch (InterruptedException ex ) {

		}

		return "";
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

		innerLayout.removeView(hsv.getPbl());
		innerLayout.addView(loadImg);
		hsv.updateImageBounds();
		hsv.addImageSize(loadImg);
	}

}

