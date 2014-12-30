package android.lessons.custom_horizontal_scroll;

/**
 * Ilya <ilya.likhobabin@gmail.com>
 */
interface ScrollHandler extends Handler {

	void scroll();

	void init(int xScroll, int yScroll, boolean isSmoothScroll);

	void reset();
}
