package cam.sabtab;
import cam.sabtab.model.WarningItem;
import cam.sabtab.model.SabControl;
import cam.sabtab.model.SabControlEvent;

import java.util.List;
import java.text.DateFormat;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
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

public class TabStatusFragment extends Fragment
{
	private static final String TAG = "TabStatusFragment";
	protected LayoutInflater inflater = null;
	private boolean paused = false;
	private SabControl sab;
	private Handler handler = new Handler(); 
	private ListView warningLV;
	private WarningAdapter warningAdapter;
	private ProgressBar warningProgress;

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state)
	{
		this.inflater = inflater;

		Log.v(TAG, "onCreateView");
		View v = inflater.inflate(R.layout.tab_status, container, false);

		//setup the sab controller with a refresher
		this.sab = new SabControl(getActivity().getApplicationContext(), new SabControlEvent() {
			public void refresh() {
				handler.removeCallbacks(updateWarningTask);
				handler.post(updateWarningTask);
			}
      });

		//get the list
		warningAdapter = new WarningAdapter(getActivity());
		warningLV = (ListView)v.findViewById(R.id.warnings);
		warningLV.setAdapter(warningAdapter);
		warningProgress = (ProgressBar)v.findViewById(R.id.warnings_progress);

		//start the update timer
		handler.post(updateWarningTask);

		return v;
	}

	@Override public void onPause()
	{
		super.onPause();
		Log.v(TAG, "onPause()");
		paused = true;
	}

	@Override public void onResume()
	{
		super.onResume();
		Log.v(TAG, "onResume()");
		paused = false;
		handler.post(updateWarningTask);
	}

	@Override public void onDestroy()
	{
		super.onDestroy();
		Log.v(TAG, "onDestroy()");
		handler.removeCallbacks(updateWarningTask);
	}

	//start the download task and re-ping ourselves for continual updates
	private Runnable updateWarningTask = new Runnable() {
		public void run() {
			if(paused) return;

			new DownloadWarningTask().execute();

			handler.removeCallbacks(updateWarningTask);
			handler.postDelayed(this, 30000);
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
