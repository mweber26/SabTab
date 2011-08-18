package cam.sabtab;
import cam.sabtab.model.HistoryItem;
import cam.sabtab.model.SabControl;
import cam.sabtab.model.SabControlEvent;

import java.util.List;

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

public class TabHistoryFragment extends Fragment
{
	private static final String TAG = "TabHistoryFragment";
	private boolean paused = false;
	protected LayoutInflater inflater = null;
	private SabControl sab;
	private Handler handler = new Handler(); 
	private ListView listView;
	private HistoryAdapter listAdapter;
	private ProgressBar listProgress;

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state)
	{
		this.inflater = inflater;

		Log.v(TAG, "onCreateView");
		View v = inflater.inflate(R.layout.tab_history, container, false);

		//setup the sab controller with a refresher
		this.sab = new SabControl(getActivity().getApplicationContext(), new SabControlEvent() {
			public void refresh() {
				handler.removeCallbacks(updateHistoryTask);
				handler.post(updateHistoryTask);
			}
      });

		//get the list
		listAdapter = new HistoryAdapter(getActivity());
		listView = (ListView)v.findViewById(R.id.list);
		listView.setAdapter(listAdapter);
		listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		listView.setOnItemClickListener(listViewClick);

		//register for the listview context menu
		registerForContextMenu(listView);

		//get the list progress
		listProgress = (ProgressBar)v.findViewById(R.id.list_progress);

		//start the update timer
		handler.post(updateHistoryTask);

		//get the details controls
		detailsScriptLog = (TextView)v.findViewById(R.id.detail_script_log);

		return v;
	}

   @Override public void onActivityCreated(Bundle state)
	{
		super.onActivityCreated(state);
		Log.v(TAG, "onActivityCreated : state==null? " + (state == null));

		if(state != null)
		{
			listView.setSelection(state.getInt("item_selected"));
			updateDetails(currentItem());
		}
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
		handler.post(updateHistoryTask);
	}

	@Override public void onDestroy()
	{
		super.onDestroy();
		Log.v(TAG, "onDestroy()");
		handler.removeCallbacks(updateHistoryTask);
	}

	@Override public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		outState.putInt("item_selected", listView.getCheckedItemPosition());
	}

	private HistoryItem currentItem()
	{
		int index = listView.getCheckedItemPosition();

		//did we hit zero items in the list while trying to update?
		if(listAdapter.getCount() == 0)
		{
			listView.setSelection(-1);
			return null;
		}

		//if we are past the end of the list then move the selection
		if(index >= listAdapter.getCount())
		{
			index = 0;
			listView.setSelection(0);
		}

		//get the selected item
		if(index != ListView.INVALID_POSITION)
			return (HistoryItem)listAdapter.getItem(index);
		else
			return null;
	}

	private TextView detailsScriptLog;
	private void updateDetails(HistoryItem item)
	{
		if(paused || item == null) return;

		detailsScriptLog.setText(item.getScriptLog());
	}

	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
	{
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
		HistoryItem item = (HistoryItem)listView.getItemAtPosition(info.position);
		menu.setHeaderTitle(item.getName());

		MenuItem retry = menu.add(R.string.history_list_retry);
		retry.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				public boolean onMenuItemClick(MenuItem item) {
					//Intent intent = new Intent(getActivity(), HostEditorActivity.class);
					//intent.putExtra(Intent.EXTRA_TITLE, host.getId());
					//getActivity().startActivityForResult(intent, REQUEST_EDIT);
					return true;
				}
		});

		MenuItem del = menu.add(R.string.history_list_delete);
		del.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				public boolean onMenuItemClick(MenuItem item) {
					//Intent intent = new Intent(getActivity(), HostEditorActivity.class);
					//intent.putExtra(Intent.EXTRA_TITLE, host.getId());
					//getActivity().startActivityForResult(intent, REQUEST_EDIT);
					return true;
				}
		});
	}

	//the listview click handler
	private AdapterView.OnItemClickListener listViewClick = new AdapterView.OnItemClickListener() {
		public synchronized void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			HistoryItem item = (HistoryItem)parent.getAdapter().getItem(position);
			updateDetails(item);
		}
	};

	//start the download task and re-ping ourselves for continual updates
	private Runnable updateHistoryTask = new Runnable() {
		public void run() {
			if(paused) return;

			new DownloadHistoryTask().execute();

			handler.removeCallbacks(updateHistoryTask);
			handler.postDelayed(this, 30000);
		}
	};

	private class HistoryAdapter extends ArrayAdapter<HistoryItem>
	{
		public HistoryAdapter(Context context)
		{
			super(context, R.layout.history_item);
		}

		@Override public View getView(int position, View convertView, ViewGroup parent)
		{
			View row;
			HistoryItem history = getItem(position);

			if(null == convertView)
				row = inflater.inflate(R.layout.history_item, null);
			else
				row = convertView;

			TextView tv = (TextView)row.findViewById(R.id.history_name);
			tv.setText(history.getName());

			tv = (TextView)row.findViewById(R.id.history_status);
			tv.setText(history.getStatus());

			tv = (TextView)row.findViewById(R.id.history_stats);
			tv.setText(history.getStats());

			return row;
		}
	}

	private class DownloadHistoryTask extends AsyncTask<Void, Void, List<HistoryItem>>
	{
		protected List<HistoryItem> doInBackground(Void... unused)
		{
			return sab.fetchHistory(0, 50);
		}

		protected void onPreExecute()
		{
			listProgress.setVisibility(View.VISIBLE);
		}

		protected void onPostExecute(List<HistoryItem> result)
		{
			if(result != null)
			{
				//reload the list
				listAdapter.clear();
				listAdapter.addAll(result);
				listAdapter.notifyDataSetChanged();
			}

			//hide the spinner
			listProgress.setVisibility(View.INVISIBLE);
		}
	}
}
