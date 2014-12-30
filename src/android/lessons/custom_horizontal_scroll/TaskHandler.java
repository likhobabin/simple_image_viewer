package android.lessons.custom_horizontal_scroll;

import android.os.AsyncTask;

/**
 * Ilya <ilya.likhobabin@gmail.com>
 */
public interface TaskHandler<Params, Progress, Result> extends Handler {

	AsyncTask<Params, Progress, Result> executeTask();

}
