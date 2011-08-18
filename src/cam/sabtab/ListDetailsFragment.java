package cam.sabtab;
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

public abstract class ListDetailsFragment<T> extends Fragment
{
	private static final String TAG = "ListDetailsFragment";
	protected LayoutInflater inflater = null;
	private ArrayList<T> items = new ArrayList<T>();
	private boolean paused = false;
	private SabControl sab;
	private Handler handler = new Handler(); 
	private ListView listView;
	private ItemAdapter listAdapter;
	private ProgressBar listProgress;
	private View detailsFrame;

	protected abstract int getRefreshRate(); 
	protected abstract int getResourceViewId(); 
	protected abstract void setupDetails(View v);
	protected abstract void updateDetails(View v, T item);
	protected abstract void updateItem(View v, T item);
	protected abstract ArrayList<T> fetchItems(SabControl sab);

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state)
	{
		this.inflater = inflater;

		Log.v(TAG, "onCreateView : state==null? " + (state == null));
		View v = inflater.inflate(getResourceViewId(), container, false);

		//setup the sab controller with a refresher
		this.sab = new SabControl(getActivity().getApplicationContext(), new SabControlEvent() {
			public void refresh() {
				handler.removeCallbacks(updateListTask);
				handler.post(updateListTask);
			}
      });
		
		setupList(v);
		detailsFrame = v.findViewById(R.id.details);
		setupDetails(detailsFrame);

		//start the update timer
		handler.post(updateListTask);

		return v;
	}

   @Override public void onActivityCreated(Bundle state)
	{
		super.onActivityCreated(state);
		Log.v(TAG, "onActivityCreated : state==null? " + (state == null));

		if(state != null)
		{
			listView.setSelection(state.getInt("item_selected"));
			updateDetailsInternal(currentItem());
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
		handler.post(updateListTask);
	}

	@Override public void onDestroy()
	{
		super.onDestroy();
		Log.v(TAG, "onDestroy()");
		handler.removeCallbacks(updateListTask);
	}

	@Override public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		outState.putInt("item_selected", listView.getCheckedItemPosition());
	}

	private T currentItem()
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
			return (T)listAdapter.getItem(index);
		else
			return null;
	}

	private void setupList(View v)
	{
		listAdapter = new ItemAdapter(getActivity(), items);

		listView = (ListView)v.findViewById(R.id.list);
		listProgress = (ProgressBar)v.findViewById(R.id.list_progress);

		listView.setAdapter(listAdapter);
		listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public synchronized void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
				T item = (T)parent.getAdapter().getItem(pos);
				updateDetailsInternal(item);
			}
		});
	}

	private void updateDetailsInternal(T item)
	{
		if(paused || item == null)
			return;
		else
			updateDetails(detailsFrame, item);
	}

	//start the download task and re-ping ourselves for continual updates
	private Runnable updateListTask = new Runnable() {
		public void run() {
			if(paused) return;

			new UpdateListTask().execute();

			handler.removeCallbacks(updateListTask);
			handler.postDelayed(this, getRefreshRate());
		}
	};

	private class ItemAdapter extends ArrayAdapter<T>
	{
		public ItemAdapter(Context context, ArrayList<T> items)
		{
			super(context, R.layout.queue_item, items);
		}

		@Override public View getView(int position, View convertView, ViewGroup parent)
		{
			View row;
			T item = getItem(position);

			if(null == convertView)
				row = inflater.inflate(R.layout.queue_item, null);
			else
				row = convertView;

			updateItem(row, item);
			return row;
		}
	}

	private class UpdateListTask extends AsyncTask<Void, Void, ArrayList<T>>
	{
		protected ArrayList<T> doInBackground(Void... unused)
		{
			return fetchItems(sab);
		}

		protected void onPreExecute()
		{
			listProgress.setVisibility(View.VISIBLE);
		}

		protected void onPostExecute(ArrayList<T> result)
		{
			if(result != null)
			{
				//store the items
				items = result;

				//reload the list
				listAdapter.clear();
				listAdapter.addAll(result);
				listAdapter.notifyDataSetChanged();

				//update details if we have a selected item
				T item = currentItem();
				if(item != null) updateDetailsInternal(item);
			}

			//hide the spinner
			listProgress.setVisibility(View.INVISIBLE);
		}
	}
}
