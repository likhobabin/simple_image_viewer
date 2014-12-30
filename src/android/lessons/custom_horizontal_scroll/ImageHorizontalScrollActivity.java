package android.lessons.custom_horizontal_scroll;

import android.app.Activity;
import android.os.Bundle;

public class ImageHorizontalScrollActivity extends Activity {

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		final ImageHorizontalScrollView chsv =
			(ImageHorizontalScrollView) findViewById(R.id.horizontalScrollView);

		chsv.setViewList(R.id.mainLayout, this);

	}
}
