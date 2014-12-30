package android.lessons.custom_horizontal_scroll;

import android.app.Activity;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.*;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;

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

		private Matrix matrix = new Matrix();

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

	private static class ImageBounds {

		public int[][] imageBoundsMatrix;

		public ImageBounds() {
			imageBoundsMatrix = new int[4][2];
			for (int i = 0; i < imageBoundsMatrix.length; i++) {
				imageBoundsMatrix[i] = new int[2];
			}
		}

		public boolean checkEdges(float[] imageMatrix) {
			return checkLeftTop(imageMatrix) && checkRightTop(imageMatrix) && checkLeftBottom(imageMatrix)
				&& checkRightBottom(imageMatrix);
		}

		public void correctEdges(float[] imageMatrix) {
			if (!checkLeftTop(imageMatrix)) {
				if (imageBoundsMatrix[0][0] + imageMatrix[2] > imageBoundsMatrix[0][0]) {
					imageMatrix[2] = imageBoundsMatrix[0][0];
				} else {
					imageMatrix[5] = imageBoundsMatrix[0][1];
				}
			}
			if (!checkRightTop(imageMatrix)) {
				float transformationX = ImageUtils.getTransformationX(imageBoundsMatrix[1][0], imageMatrix);

				if (transformationX < imageBoundsMatrix[1][0]) {
					imageMatrix[2] += imageBoundsMatrix[1][0] - transformationX;
				} else {
					imageMatrix[5] = imageBoundsMatrix[1][1];
				}
			}
			if (!checkLeftBottom(imageMatrix)) {
				float transformationY = ImageUtils.getTransformationY(imageBoundsMatrix[2][1], imageMatrix);
				if (transformationY < imageBoundsMatrix[2][1]) {
					imageMatrix[5] += imageBoundsMatrix[2][1] - transformationY;
				} else {
					imageMatrix[2] = imageBoundsMatrix[2][0];
				}
			}
			if (!checkRightBottom(imageMatrix)) {
				float transformationX = ImageUtils.getTransformationX(imageBoundsMatrix[3][0], imageMatrix);
				float transformationY = ImageUtils.getTransformationY(imageBoundsMatrix[3][1], imageMatrix);
				if (transformationX < imageBoundsMatrix[3][0]) {
					imageMatrix[2] += imageBoundsMatrix[3][0] - transformationX;
				} else {
					imageMatrix[5] += imageBoundsMatrix[3][1] - transformationY;
				}
			}
		}

		public void setEdges(ImageView imageView, int[] displayMetrics) {
			int[] imageSize = new int[2];

			ImageUtils.getImageSize(imageView, imageSize);

			imageBoundsMatrix[0][0] = 0;
			imageBoundsMatrix[0][1] = 0;

			imageBoundsMatrix[1][0] = displayMetrics[0];
			imageBoundsMatrix[1][1] = imageBoundsMatrix[0][1];

			imageBoundsMatrix[2][0] = 0;
			imageBoundsMatrix[2][1] = imageSize[1];

			imageBoundsMatrix[3][0] = displayMetrics[0];
			imageBoundsMatrix[3][1] = imageSize[1];
		}

		private boolean checkLeftTop(float[] imageMatrix) {
			return imageBoundsMatrix[0][0] + imageMatrix[2] <= imageBoundsMatrix[0][0]
				&& imageBoundsMatrix[0][1] + imageMatrix[5] <= imageBoundsMatrix[0][1];

		}

		private boolean checkRightTop(float[] imageMatrix) {
			float transformationX = ImageUtils.getTransformationX(imageBoundsMatrix[1][0], imageMatrix);
			return transformationX >= imageBoundsMatrix[1][0]
				&& imageBoundsMatrix[1][1] + imageMatrix[5] <= imageBoundsMatrix[1][1];

		}

		private boolean checkLeftBottom(float[] imageMatrix) {
			float transformationY = ImageUtils.getTransformationY(imageBoundsMatrix[2][1], imageMatrix);
			return imageBoundsMatrix[2][0] + imageMatrix[2] <= imageBoundsMatrix[2][0]
				&& transformationY >= imageBoundsMatrix[2][1];

		}

		private boolean checkRightBottom(float[] imageMatrix) {
			float transformationX
				= ImageUtils.getTransformationX(imageBoundsMatrix[3][0], imageMatrix);

			float transformationY
				= ImageUtils.getTransformationY(imageBoundsMatrix[3][1], imageMatrix);

			return transformationX >= imageBoundsMatrix[3][0] && transformationY >= imageBoundsMatrix[3][1];
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

	private static class ImageLoaderHandler implements TaskHandler {

		private final ImageHorizontalScrollView hsv;

		private final LinearLayout hsvLayout;

		private Integer[] taskParams;

		public ImageLoaderHandler(ImageHorizontalScrollView hsv, LinearLayout hsvLayout) {
			this.hsv = hsv;
			this.hsvLayout = hsvLayout;
		}

		public void init(Integer... taskParams) {
			this.taskParams = taskParams;
		}

		@Override
		public boolean isSchedule() {
			return taskParams != null && taskParams.length != 0;
		}

		@Override
		public  void reset() {
			taskParams = null;
		}

		@Override
		public void executeTask() {
			new ImageLoaderTask(hsv, hsvLayout).execute(taskParams);
		}

	}

	private MotionProcessType motionProcess = MotionProcessType.NONE;

	private ImageBounds imageBounds = new ImageBounds();

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

	private ImageLoaderTask imgLoader;

	private LinearLayout scrollViewLayout;

	private ScrollHandler scrollHandler;

	private ImageLoaderHandler taskHandler;

	private final ImageItem[] imageItems = {
		new ImageItem(IMG_INDEX_MASK),
		new ImageItem(1 ^ IMG_INDEX_MASK),
		new ImageItem(2 ^ IMG_INDEX_MASK),
		new ImageItem(3 ^ IMG_INDEX_MASK),
		new ImageItem(4 ^ IMG_INDEX_MASK)
	};

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

	public void setViewList(Integer linearLayoutId, Activity activity) {
		displayMetrics = ImageUtils.getDisplayMetric(activity);
		scrollViewLayout = (LinearLayout) findViewById(linearLayoutId);
		scrollViewLayout.addView(getPbl());

		imgLoader = new ImageLoaderTask(this, scrollViewLayout);
		imgLoader.execute(imageIndex, linearLayoutId);
	}

	public void updateImageBounds() {
		ImageView currImage = (ImageView) findViewById(imageIndex ^ IMG_INDEX_MASK);
		imageBounds.setEdges(currImage, displayMetrics);
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
						|| !imageBounds.checkEdges(mx_values)) {

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

					if (!imageBounds.checkEdges(mx_values)) {
						imageBounds.correctEdges(mx_values);
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
			getImageLoaderHandler().executeTask();
			getImageLoaderHandler().reset();
		}
	}

	int getImageIndex() {
		return imageIndex;
	}

	int[] getDisplayMetrics() {
		return displayMetrics;
	}

	View getPbl() {
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

		boolean isSmoothScroll = false;

		Log.d("IHSV", "processScroll imageIndex: " + imageIndex + " isForward: " + isForward + " isPaging: " + isPaging);

		if (isPaging) {
			if (isForward) {
				isSmoothScroll = imageIndex + 1 != imageItems.length;
				imageIndex = ++imageIndex % imageItems.length;
			} else {
				isSmoothScroll = imageIndex - 1 >= 0;
				imageIndex = imageIndex - 1 < 0 ? imageItems.length - 1 : --imageIndex;
			}

			if (!imageItems[imageIndex].isLoad) {
				scrollViewLayout.addView(getPbl());
				if (isSmoothScroll) {
					getScrollHandler().init(imageIndex * displayMetrics[0], 0, true);
				} else {
					getScrollHandler().init(imageIndex * displayMetrics[0], 0, true);
				}
				getImageLoaderHandler().init(imageItems[imageIndex].id);

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

	public PointF getStartPoint() {
		if (startPoint == null) {
			startPoint = new PointF();
		}
		return startPoint;
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
			taskHandler = new ImageLoaderHandler(this, scrollViewLayout);
		}

		return taskHandler;
	}

}
