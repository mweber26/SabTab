package cam.sabtab;
import cam.sabtab.model.WarningItem;
import cam.sabtab.model.SabControl;
import cam.sabtab.model.SabControlEvent;

import java.util.List;
import java.text.DateFormat;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.util.Log;

public class WarningsActivity extends Activity
{
	private static final String TAG = "WarningsActivity";
	protected LayoutInflater inflater = null;
	private SabControl sab;
	private Handler handler = new Handler(); 
	private ListView warningLV;
	private WarningAdapter warningAdapter;
	private ProgressBar warningProgress;

	@Override public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Log.v(TAG, "onCreateView");
		setContentView(R.layout.warnings);
		inflater = LayoutInflater.from(this);

		//setup the sab controller with a refresher
		this.sab = new SabControl(getApplicationContext(), new SabControlEvent() {
			public void refresh() {
				handler.removeCallbacks(updateWarningTask);
				handler.post(updateWarningTask);
			}
      });

		//get the list
		warningAdapter = new WarningAdapter(this);
		warningLV = (ListView)findViewById(R.id.list);
		warningLV.setAdapter(warningAdapter);
		warningProgress = (ProgressBar)findViewById(R.id.list_progress);

		//start the update timer
		handler.post(updateWarningTask);
	}

	@Override protected void onPause()
	{
		super.onPause();
		Log.v(TAG, "onPause()");
	}

	@Override protected void onResume()
	{
		super.onResume();
		Log.v(TAG, "onResume()");
		handler.post(updateWarningTask);
	}

	@Override protected void onDestroy()
	{
		super.onDestroy();
		Log.v(TAG, "onDestroy()");
		handler.removeCallbacks(updateWarningTask);
	}

	//start the download task and re-ping ourselves for continual updates
	private Runnable updateWarningTask = new Runnable() {
		public void run() {
			new DownloadWarningTask().execute();

			handler.removeCallbacks(updateWarningTask);
		}
	};

	private class WarningAdapter extends ArrayAdapter<WarningItem>
	{
		public WarningAdapter(Context context)
		{
			super(context, R.layout.warning_item);
		}

		@Override public View getView(int position, View convertView, ViewGroup parent)
		{
			DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getContext());
			DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(getContext());
			View row;
			WarningItem item = getItem(position);

			if(null == convertView)
				row = inflater.inflate(R.layout.warning_item, null);
			else
				row = convertView;

			TextView tv = (TextView)row.findViewById(R.id.text);
			tv.setText(item.getText());

			tv = (TextView)row.findViewById(R.id.date);
			tv.setText(dateFormat.format(item.getDate()) + " " + timeFormat.format(item.getDate()));

			tv = (TextView)row.findViewById(R.id.level);
			tv.setText(item.getLevel());

			return row;
		}
	}

	private class DownloadWarningTask extends AsyncTask<Void, Void, List<WarningItem>>
	{
		protected List<WarningItem> doInBackground(Void... unused)
		{
			return sab.fetchWarnings();
		}

		protected void onPreExecute()
		{
			warningProgress.setVisibility(View.VISIBLE);
		}

		protected void onPostExecute(List<WarningItem> result)
		{
			if(result != null)
			{
				Log.v(TAG, "onPostExecute");

				//reload the list
				warningAdapter.clear();
				warningAdapter.addAll(result);
				warningAdapter.notifyDataSetChanged();
			}

			//hide the spinner
			warningProgress.setVisibility(View.INVISIBLE);
		}
	}
}
