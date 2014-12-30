package android.lessons.custom_horizontal_scroll;

import android.app.Activity;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.*;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.List;

import static android.lessons.custom_horizontal_scroll.ImageUtils.*;

/**
 * Ilya <ilya.likhobabin@gmail.com>
 */
public class ImageHorizontalScrollView extends HorizontalScrollView implements GestureDetector.OnGestureListener {

	private static enum MotionProcessType {
		ZOOM,
		DRAG,
		NONE
	}

	static class ImageItem {

		public int id;

		public boolean isLoad;

		public int[] origDimension = new int[2];

		private Matrix savedMatrix;

		private Matrix matrix;

		ImageItem(int id) {
			this.id = id;
		}

		Matrix getSavedMatrix() {
			if (savedMatrix == null) {
				savedMatrix = new Matrix();
			}
			return savedMatrix;
		}

		public Matrix getMatrix() {
			if (matrix == null) {
				matrix = new Matrix();
			}
			return matrix;
		}

	}

	private static class ImageBorders {

		public int[][] imageBordersMatrix;

		public ImageBorders() {
			imageBordersMatrix = new int[4][2];
			for (int i = 0; i < imageBordersMatrix.length; i++) {
				imageBordersMatrix[i] = new int[2];
			}
		}

		public boolean checkBorders(float[] imageMatrix) {
			return checkLeftTop(imageMatrix) && checkRightTop(imageMatrix) && checkLeftBottom(imageMatrix)
				&& checkRightBottom(imageMatrix);
		}

		public void correctBorders(float[] imageMatrix) {
			if (!checkLeftTop(imageMatrix)) {
				if (imageBordersMatrix[0][0] + imageMatrix[2] > imageBordersMatrix[0][0]) {
					imageMatrix[2] = imageBordersMatrix[0][0];
				} else {
					imageMatrix[5] = imageBordersMatrix[0][1];
				}
			}
			if (!checkRightTop(imageMatrix)) {
				float transformationX = ImageUtils.getTransformationX(imageBordersMatrix[1][0], imageMatrix);

				if (transformationX < imageBordersMatrix[1][0]) {
					imageMatrix[2] += imageBordersMatrix[1][0] - transformationX;
				} else {
					imageMatrix[5] = imageBordersMatrix[1][1];
				}
			}
			if (!checkLeftBottom(imageMatrix)) {
				float transformationY = ImageUtils.getTransformationY(imageBordersMatrix[2][1], imageMatrix);
				if (transformationY < imageBordersMatrix[2][1]) {
					imageMatrix[5] += imageBordersMatrix[2][1] - transformationY;
				} else {
					imageMatrix[2] = imageBordersMatrix[2][0];
				}
			}
			if (!checkRightBottom(imageMatrix)) {
				float transformationX = ImageUtils.getTransformationX(imageBordersMatrix[3][0], imageMatrix);
				float transformationY = ImageUtils.getTransformationY(imageBordersMatrix[3][1], imageMatrix);
				if (transformationX < imageBordersMatrix[3][0]) {
					imageMatrix[2] += imageBordersMatrix[3][0] - transformationX;
				} else {
					imageMatrix[5] += imageBordersMatrix[3][1] - transformationY;
				}
			}
		}

		public void setBorders(ImageView imageView, int[] displayMetrics) {
			int[] imageSize = new int[2];

			ImageUtils.getImageSize(imageView, imageSize);

			imageBordersMatrix[0][0] = 0;
			imageBordersMatrix[0][1] = 0;

			imageBordersMatrix[1][0] = displayMetrics[0];
			imageBordersMatrix[1][1] = imageBordersMatrix[0][1];

			imageBordersMatrix[2][0] = 0;
			imageBordersMatrix[2][1] = imageSize[1];

			imageBordersMatrix[3][0] = displayMetrics[0];
			imageBordersMatrix[3][1] = imageSize[1];
		}

		private boolean checkLeftTop(float[] imageMatrix) {
			return imageBordersMatrix[0][0] + imageMatrix[2] <= imageBordersMatrix[0][0]
				&& imageBordersMatrix[0][1] + imageMatrix[5] <= imageBordersMatrix[0][1];

		}

		private boolean checkRightTop(float[] imageMatrix) {
			float transformationX = ImageUtils.getTransformationX(imageBordersMatrix[1][0], imageMatrix);
			return transformationX >= imageBordersMatrix[1][0]
				&& imageBordersMatrix[1][1] + imageMatrix[5] <= imageBordersMatrix[1][1];

		}

		private boolean checkLeftBottom(float[] imageMatrix) {
			float transformationY = ImageUtils.getTransformationY(imageBordersMatrix[2][1], imageMatrix);
			return imageBordersMatrix[2][0] + imageMatrix[2] <= imageBordersMatrix[2][0]
				&& transformationY >= imageBordersMatrix[2][1];

		}

		private boolean checkRightBottom(float[] imageMatrix) {
			float transformationX
				= ImageUtils.getTransformationX(imageBordersMatrix[3][0], imageMatrix);

			float transformationY
				= ImageUtils.getTransformationY(imageBordersMatrix[3][1], imageMatrix);

			return transformationX >= imageBordersMatrix[3][0] && transformationY >= imageBordersMatrix[3][1];
		}

	}

	private static class CustomScrollHandler implements ScrollHandler {

		private final HorizontalScrollView scroller;

		private int xScroll;

		private int yScroll;

		private boolean isSmoothScroll;

		private boolean isSchedule;

		public CustomScrollHandler(HorizontalScrollView scroller) {
			this.scroller = scroller;
			reset();
		}

		@Override
		public void scroll() {
			if (isSmoothScroll) {
				scroller.smoothScrollTo(xScroll, yScroll);
			} else {
				scroller.scrollTo(xScroll, yScroll);
			}
		}

		@Override
		public boolean isSchedule() {
			return isSchedule;
		}

		@Override
		public void init(int xScroll, int yScroll, boolean isSmoothScroll) {
			this.xScroll = xScroll;
			this.yScroll = yScroll;
			this.isSmoothScroll = isSmoothScroll;
			this.isSchedule = true;
		}

		@Override
		public void reset() {
			this.xScroll = -1;
			this.yScroll = -1;
			this.isSmoothScroll = false;
			this.isSchedule = false;
		}

	}

	private static class ImageLoaderHandler implements TaskHandler<String, String, String> {

		private final ImageHorizontalScrollView hsv;

		private final LinearLayout hsvLayout;

		private String[] taskParams;

		private boolean isSchedule;

		public ImageLoaderHandler(ImageHorizontalScrollView hsv, LinearLayout hsvLayout) {
			this.hsv = hsv;
			this.hsvLayout = hsvLayout;
		}

		public void init(String... taskParams) {
			this.taskParams = taskParams;
			this.isSchedule = true;
		}

		@Override
		public boolean isSchedule() {
			return isSchedule;
		}

		@Override
		public  void reset() {
			taskParams = null;
			isSchedule = false;
		}

		@Override
		public AsyncTask<String, String, String> executeTask() {
			return new ImageLoaderTask(hsv, hsvLayout).execute(taskParams);
		}

	}

	private MotionProcessType motionProcess = MotionProcessType.NONE;

	private final ImageBorders imageBorders = new ImageBorders();

	private final GestureDetector gestureDetector;

	private List<int[]> imagesSizeList;

	private int[] displayMetrics;

	private int imageIndex;

	private int ev1X;

	private int ev2X;

	private PointF midPoint;

	private PointF startPoint;

	private PointF ffPoint;

	private PointF sfPoint;

	private float oldDist;

	private View pbl;

	private LinearLayout horizScrollViewLayout;

	private ScrollHandler scrollHandler;

	private ImageLoaderHandler taskHandler;

	private AsyncTask<String, String, String> progressTask;

	private ImageItem[] imageItems;

	{
		imageItems = new ImageItem[IMG_URL_ARRAY.length];
		for (int i=0; i < imageItems.length; i++) {
			imageItems[i] = new ImageItem(i ^ IMG_INDEX_MASK);
		}
	}

	public ImageHorizontalScrollView(Context context) {
		super(context);
		gestureDetector = new GestureDetector(context, this);
	}

	public ImageHorizontalScrollView(Context context, AttributeSet attrs) {
		super(context, attrs, R.attr.horizontalScrollViewStyle);
		gestureDetector = new GestureDetector(context, this);
	}

	public ImageHorizontalScrollView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		gestureDetector = new GestureDetector(context, this);
	}

	public void setViewList(Integer horizScrollViewLayoutId, Activity activity) {
		displayMetrics = ImageUtils.getDisplayMetrics(activity);
		horizScrollViewLayout = (LinearLayout) findViewById(horizScrollViewLayoutId);
		horizScrollViewLayout.addView(getPbl());

		getImageLoaderHandler().init();
	}

	public void updateImageBounds() {
		ImageView currImage = (ImageView) findViewById(imageIndex ^ IMG_INDEX_MASK);
		imageBorders.setBorders(currImage, displayMetrics);
	}

	public ImageItem[] getImageItems() {
		return imageItems;
	}

	public void addImageSize(ImageView imageView) {
		ImageUtils.getImageSize(imageView, imageItems[imageIndex].origDimension);

		int[] dstDimension = ImageUtils.createDimension();
		ImageUtils.getImageSize(imageView, dstDimension);
		getImageSizeList().add(dstDimension);
	}

	public int getImageIndex() {
		return imageIndex;
	}

	public int[] getDisplayMetrics() {
		return displayMetrics;
	}

	public View getPbl() {
		if (pbl == null) {
			LayoutInflater inflater = (LayoutInflater) getContext().getSystemService
				(Context.LAYOUT_INFLATER_SERVICE);

			pbl = inflater.inflate(R.layout.progress_bar, this, false);
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(displayMetrics[0],
				displayMetrics[1]);

			lp.gravity = Gravity.FILL_HORIZONTAL | Gravity.CENTER_VERTICAL;
			pbl.setLayoutParams(lp);
		}
		return pbl;
	}

	public ProgressBar getPb() {
		return (ProgressBar) getPbl().findViewById(R.id.progress_bar);
	}

	public PointF getStartPoint() {
		if (startPoint == null) {
			startPoint = new PointF();
		}
		return startPoint;
	}

	@Override
	public boolean onDown(MotionEvent event) {
		ImageItem imageItem = imageItems[imageIndex];

		motionProcess = !imageItem.getMatrix().isIdentity() ? MotionProcessType.DRAG : MotionProcessType.NONE;
		getStartPoint().set(event.getX(), event.getY());
		imageItem.getSavedMatrix().set(imageItem.getMatrix());

		Log.d("IHSV", "GestureDetectorListener OnDown event");

		return true;
	}

	@Override
	public void onShowPress(MotionEvent e) {
		Log.d("IHSV", "GestureDetectorListener onShowPress event");
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		Log.d("IHSV", "GestureDetectorListener onSingleTapUp event");
		return true;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		ev1X = (int) e1.getAxisValue(MotionEvent.AXIS_X);
		ev2X = (int) e2.getAxisValue(MotionEvent.AXIS_X);

		Log.d("IHSV", "GestureDetectorListener onScroll event. " +
			"Info scrollX " + getScrollX() + " Info distanceX " +
			distanceX + " Info ev1X " + ev1X + " Info ev2X " + ev2X);

		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		Log.d("IHSV", "GestureDetectorListener onLongPress event");
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		Log.d("IHSV", "GestureDetectorListener onFling event. " +
			"Info scrollX " + getScrollX() +
			" Info velocityX " + velocityX +
			" Info ev1X " + e1.getAxisValue(MotionEvent.AXIS_X) +
			" Info ev2X " + e2.getAxisValue(MotionEvent.AXIS_X));

		ev1X = (int) e1.getAxisValue(MotionEvent.AXIS_X);
		ev2X = (int) e2.getAxisValue(MotionEvent.AXIS_X);

		processScroll();

		return true;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (progressTask != null && (progressTask.getStatus() == AsyncTask.Status.RUNNING
			|| progressTask.getStatus() == AsyncTask.Status.PENDING)) {
			return true;
		}

		if (motionProcess != MotionProcessType.ZOOM &&
			motionProcess != MotionProcessType.DRAG &&
			super.onTouchEvent(event) &&
			gestureDetector.onTouchEvent(event)) {

			return true;
		}

		ImageItem imageItem = imageItems[imageIndex];
		ImageView imageView = (ImageView) findViewById(imageItem.id);

		if (imageView == null) {
			return true;
		}

		int actionMask = event.getAction() & event.getActionMasked();

		switch (actionMask) {
			case MotionEvent.ACTION_DOWN: {
				Log.d("IHSV_OnTouch", "ACTION_DOWN");

				break;
			}

			case MotionEvent.ACTION_POINTER_DOWN: {
				Log.d("IHSV_OnTouch", "ACTION_POINTER_DOWN");

				if (event.getPointerCount() != 2) {
					break;
				}

				motionProcess = MotionProcessType.ZOOM;
				updateFingerPoints(event);

				oldDist = ImageUtils.calcDistance(getFfPoint(), getSfPoint());
				imageItem.getSavedMatrix().set(imageItem.getMatrix());
				ImageUtils.calculateMidPoint(getFfPoint(), getSfPoint(), getMidPoint());

				break;
			}

			case MotionEvent.ACTION_POINTER_UP: {
				Log.d("IHSV_OnTouch", "ACTION_POINTER_UP");

				if (event.getPointerCount() != 1) {
					break;
				}

				motionProcess = MotionProcessType.NONE;
				break;
			}

			case MotionEvent.ACTION_MOVE: {
				Log.d("IHSV_OnTouch", "ACTION_MOVE");

				if (event.getPointerCount() > 2) {
					break;
				}
				float[] mx_values = new float[9];
				if (event.getPointerCount() == 2 && motionProcess == MotionProcessType.ZOOM) {
					Log.d("IHSV_OnTouch", "Event onZoom");
					updateFingerPoints(event);
					float newDist = ImageUtils.calcDistance(getFfPoint(), getSfPoint());
					imageItem.getMatrix().set(imageItem.getSavedMatrix());
					float scale = newDist / oldDist;

					imageItem.getMatrix().getValues(mx_values);
					mx_values[0] *= scale;
					mx_values[4] *= scale;

					float preScaleWidth = mx_values[0] * imageItem.origDimension[0];
					float preScaleHeight = mx_values[4] * imageItem.origDimension[1];

					if (preScaleWidth < imageItem.origDimension[0] || preScaleHeight < imageItem.origDimension[1]
						|| !imageBorders.checkBorders(mx_values)) {

						imageItem.getMatrix().reset();
					} else {
						imageItem.getMatrix().postScale(scale, scale, midPoint.x, midPoint.y);
					}
				} else if (event.getPointerCount() == 1 && motionProcess == MotionProcessType.DRAG) {
					Log.d("IHSV_OnTouch", "Event onDrag");

					imageItem.getMatrix().set(imageItem.getSavedMatrix());
					imageItem.getMatrix().getValues(mx_values);
					mx_values[2] += event.getX() - getStartPoint().x;
					mx_values[5] += event.getY() - getStartPoint().y;

					if (!imageBorders.checkBorders(mx_values)) {
						imageBorders.correctBorders(mx_values);
						imageItem.getMatrix().setValues(mx_values);
					} else {
						imageItem.getMatrix().postTranslate(
							event.getX() - getStartPoint().x,
							event.getY() - getStartPoint().y
						);
					}
				}
				break;
			}

			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				Log.d("IHSV_OnTouch", "Event ACTION_UP or ACTION_CANCEL");

				if (motionProcess != MotionProcessType.ZOOM && motionProcess != MotionProcessType.DRAG) {
					processScroll();
				} else {
					motionProcess = MotionProcessType.NONE;
				}
				break;
		}

		imageView.setImageMatrix(imageItem.getMatrix());

		return true;
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);

		if (getScrollHandler().isSchedule()) {
			getScrollHandler().scroll();
			getScrollHandler().reset();
		}

		if (getImageLoaderHandler().isSchedule()) {
			progressTask = getImageLoaderHandler().executeTask();
			getImageLoaderHandler().reset();
		}
	}

	private List<int[]> getImageSizeList() {
		if (imagesSizeList == null) {
			imagesSizeList = new ArrayList<int[]>();
		}
		return imagesSizeList;
	}

	private void processScroll() {
		boolean isForward = ev1X > ev2X;
		boolean isPaging = isForward ? getImageSizeList().get(imageIndex)[0] / 2 >= ev2X :
			getImageSizeList().get(imageIndex)[0] / 2 <= ev2X;

		boolean backToTail = !isForward && imageIndex - 1 < 0;
		boolean isSmoothScroll = false;

		Log.d("IHSV", "processScroll imageIndex: " + imageIndex + " isForward: " + isForward + " isPaging: " + isPaging);

		if (isPaging && !backToTail) {
			if (isForward) {
				isSmoothScroll = imageIndex + 1 != imageItems.length;
				imageIndex = ++imageIndex % imageItems.length;
			} else {
				isSmoothScroll = imageIndex - 1 >= 0;
				--imageIndex;
			}

			if (!imageItems[imageIndex].isLoad) {
				horizScrollViewLayout.addView(getPbl());
				if (isSmoothScroll) {
					getScrollHandler().init(imageIndex * displayMetrics[0], 0, true);
				} else {
					getScrollHandler().init(imageIndex * displayMetrics[0], 0, false);
				}
				getImageLoaderHandler().init(String.valueOf(isForward));

			} else  {
				if (isSmoothScroll) {
					smoothScrollTo(imageIndex * displayMetrics[0], 0);
				} else {
					scrollTo(imageIndex * displayMetrics[0], 0);
				}
			}
		} else {
			smoothScrollTo(imageIndex * displayMetrics[0], 0);
		}
	}

	private PointF getFfPoint() {
		if (ffPoint == null) {
			ffPoint = new PointF();
		}
		return ffPoint;
	}

	private PointF getSfPoint() {
		if (sfPoint == null) {
			sfPoint = new PointF();
		}
		return sfPoint;
	}

	private PointF getMidPoint() {
		if (midPoint == null) {
			midPoint = new PointF();
		}
		return midPoint;
	}

	private void updateFingerPoints(MotionEvent event) {
		getFfPoint().set(event.getX(0), event.getY(0));
		getSfPoint().set(event.getX(1), event.getY(1));
	}

	private ScrollHandler getScrollHandler() {
		if (scrollHandler == null) {
			scrollHandler = new CustomScrollHandler(this);
		}
		return scrollHandler;
	}

	private ImageLoaderHandler getImageLoaderHandler() {
		if (taskHandler == null) {
			taskHandler = new ImageLoaderHandler(this, horizScrollViewLayout);
		}

		return taskHandler;
	}

}
