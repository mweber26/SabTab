package cam.sabtab;
import cam.sabtab.model.Queue;
import cam.sabtab.model.QueueItem;
import cam.sabtab.model.SabControl;
import cam.sabtab.model.SabControlEvent;

import java.util.ArrayList;
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
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.util.Log;

public class TabQueueFragment extends Fragment
{
	private static final String TAG = "TabQueueFragment";
	protected LayoutInflater inflater = null;
	private boolean paused = false;
	private SabControl sab;
	private Handler handler = new Handler(); 
	private ListView listView;
	private QueueAdapter listAdapter;
	private ProgressBar listProgress;
	private View detailsFrame;
	private Button btnItemMoveUp, btnItemMoveDown, btnItemDelete, btnItemPause, btnItemResume;

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state)
	{
		this.inflater = inflater;

		Log.v(TAG, "onCreateView");
		View v = inflater.inflate(R.layout.tab_queue, container, false);

		//setup the sab controller with a refresher
		this.sab = new SabControl(getActivity().getApplicationContext(), new SabControlEvent() {
			public void refresh() {
				handler.removeCallbacks(updateQueueTask);
				handler.post(updateQueueTask);
			}
      });

		//get the list
		listAdapter = new QueueAdapter(getActivity());
		listView = (ListView)v.findViewById(R.id.list);
		listView.setAdapter(listAdapter);
		listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		listView.setOnItemClickListener(listViewClick);

		//register for the listview context menu
		//registerForContextMenu(listView);

		//conttrols
		listProgress = (ProgressBar)v.findViewById(R.id.list_progress);
		detailsFrame = v.findViewById(R.id.details);
		btnItemMoveUp = (Button)v.findViewById(R.id.item_move_up);
		btnItemMoveDown = (Button)v.findViewById(R.id.item_move_down);
		btnItemDelete = (Button)v.findViewById(R.id.item_delete);
		btnItemPause = (Button)v.findViewById(R.id.item_pause);
		btnItemResume = (Button)v.findViewById(R.id.item_resume);

		btnItemPause.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				QueueItem item = currentItem();
				if(item == null) return;
				sab.pauseSlot(item);
			}
		});

		btnItemResume.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				QueueItem item = currentItem();
				if(item == null) return;
				sab.resumeSlot(item);
			}
		});

		//start the update timer
		handler.post(updateQueueTask);

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
		handler.post(updateQueueTask);
	}

	@Override public void onDestroy()
	{
		super.onDestroy();
		Log.v(TAG, "onDestroy()");
		handler.removeCallbacks(updateQueueTask);
	}

	private QueueItem currentItem()
	{
		if(listView.getCheckedItemPosition() != ListView.INVALID_POSITION)
			return (QueueItem)listAdapter.getItem(listView.getCheckedItemPosition());
		else
			return null;
	}

	private void updateDetails(QueueItem item)
	{
		if(paused) return;

		TextView id = (TextView)detailsFrame.findViewById(R.id.details_id);
		TextView name = (TextView)detailsFrame.findViewById(R.id.details_name);
		TextView status = (TextView)detailsFrame.findViewById(R.id.details_status);
		TextView time = (TextView)detailsFrame.findViewById(R.id.details_time);
		TextView eta = (TextView)detailsFrame.findViewById(R.id.details_eta);
		TextView left = (TextView)detailsFrame.findViewById(R.id.details_left);
		TextView total = (TextView)detailsFrame.findViewById(R.id.details_total);
		TextView age = (TextView)detailsFrame.findViewById(R.id.details_age);

		id.setText(item.getId());
		name.setText(item.getName());
		status.setText(item.getStatus());
		time.setText(item.getTimeLeft());
		eta.setText(item.getEta());
		left.setText(item.getSizeLeft());
		total.setText(item.getSizeTotal());
		age.setText(item.getAge());

		btnItemResume.setVisibility(item.isPaused() ? View.VISIBLE : View.GONE);
		btnItemPause.setVisibility(item.isPaused() ? View.GONE : View.VISIBLE);

		loadPrioritySpinner(item);
		loadCategorySpinner(item);
		loadUnpackSpinner(item);
		loadScriptSpinner(item);

		detailsFrame.setVisibility(View.VISIBLE);
	}

	private void loadPrioritySpinner(QueueItem item)
	{
		ArrayList<String> arr = new ArrayList<String>();
		arr.add(getString(R.string.priority_force));
		arr.add(getString(R.string.priority_high));
		arr.add(getString(R.string.priority_normal));
		arr.add(getString(R.string.priority_low));

		Spinner spin = (Spinner)detailsFrame.findViewById(R.id.queue_priority);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity().getApplicationContext(),
			android.R.layout.simple_spinner_item, arr);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		spin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> arg0, View view, int position, long id) {
			}

			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});

		spin.setAdapter(adapter);

		if(item.getPriority().equals("Force")) spin.setSelection(0);
		if(item.getPriority().equals("High")) spin.setSelection(1);
		if(item.getPriority().equals("Normal")) spin.setSelection(2);
		if(item.getPriority().equals("Low")) spin.setSelection(3);
	}

	private void loadCategorySpinner(QueueItem item)
	{
		ArrayList<String> cats = item.getQueue().getCategories();

		Spinner spin = (Spinner)detailsFrame.findViewById(R.id.queue_category);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity().getApplicationContext(),
			android.R.layout.simple_spinner_item, cats);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		spin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> arg0, View view, int position, long id) {
			}

			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});

		spin.setAdapter(adapter);

		for(int i = 0; i < cats.size(); i++)
		{
			if(cats.get(i).equals(item.getCategory()))
			{
				spin.setSelection(i);
				break;
			}
		}
	}

	private void loadUnpackSpinner(QueueItem item)
	{
		ArrayList<String> arr = new ArrayList<String>();
		arr.add(getString(R.string.unpack1));
		arr.add(getString(R.string.unpack2));
		arr.add(getString(R.string.unpack3));
		arr.add(getString(R.string.unpack4));

		Spinner spin = (Spinner)detailsFrame.findViewById(R.id.queue_unpack);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity().getApplicationContext(),
			android.R.layout.simple_spinner_item, arr);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		spin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> arg0, View view, int position, long id) {
			}

			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});

		spin.setAdapter(adapter);
		spin.setSelection(item.getUnpackOptionIndex());
	}

	private void loadScriptSpinner(QueueItem item)
	{
		ArrayList<String> scripts = item.getQueue().getScripts();

		Spinner spin = (Spinner)detailsFrame.findViewById(R.id.queue_script);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity().getApplicationContext(),
			android.R.layout.simple_spinner_item, scripts);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		spin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> arg0, View view, int position, long id) {
			}

			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});

		spin.setAdapter(adapter);

		for(int i = 0; i < scripts.size(); i++)
		{
			if(scripts.get(i).equals(item.getScript()))
			{
				spin.setSelection(i);
				break;
			}
		}
	}

	//the listview click handler
	private AdapterView.OnItemClickListener listViewClick = new AdapterView.OnItemClickListener() {
		public synchronized void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			QueueItem item = (QueueItem)parent.getAdapter().getItem(position);
			updateDetails(item);
		}
	};

	//start the download task and re-ping ourselves for continual updates
	private Runnable updateQueueTask = new Runnable() {
		public void run() {
			if(paused) return;

			new DownloadQueueTask().execute();

			handler.removeCallbacks(updateQueueTask);
			handler.postDelayed(this, 10000);
		}
	};

	private class QueueAdapter extends ArrayAdapter<QueueItem>
	{
		public QueueAdapter(Context context)
		{
			super(context, R.layout.queue_item);
		}

		@Override public View getView(int position, View convertView, ViewGroup parent)
		{
			View row;
			QueueItem item = getItem(position);

			if(null == convertView)
				row = inflater.inflate(R.layout.queue_item, null);
			else
				row = convertView;

			TextView tv = (TextView)row.findViewById(R.id.queue_name);
			tv.setText(item.getName());

			tv = (TextView)row.findViewById(R.id.queue_status);
			tv.setText(item.getStatus());

			tv = (TextView)row.findViewById(R.id.queue_stats);
			tv.setText(item.getStats());

			tv = (TextView)row.findViewById(R.id.queue_time);
			tv.setText(item.getTimeLeft());

			ProgressBar progress = (ProgressBar)row.findViewById(R.id.queue_progress);
			progress.setMax(100);
			progress.setProgress(item.getProgress());

			return row;
		}
	}

	private class DownloadQueueTask extends AsyncTask<Void, Void, Queue>
	{
		protected Queue doInBackground(Void... unused)
		{
			Log.v(TAG, "downloadQueue");
			return sab.fetchQueue(0, 50);
		}

		protected void onPreExecute()
		{
			listProgress.setVisibility(View.VISIBLE);
		}

		protected void onPostExecute(Queue result)
		{
			if(result != null)
			{
				//reload the list
				listAdapter.clear();
				listAdapter.addAll(result.getSlots());
				listAdapter.notifyDataSetChanged();

				//update details if we have a selected item
				QueueItem item = currentItem();
				if(item != null) updateDetails(item);
			}

			//hide the spinner
			listProgress.setVisibility(View.INVISIBLE);
		}
	}
}
