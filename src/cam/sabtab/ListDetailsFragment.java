package cam.sabtab;
import cam.sabtab.model.SabEntity;
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
import android.content.SharedPreferences;
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

public abstract class ListDetailsFragment<T extends SabEntity> extends Fragment
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
	private TextView listNoItems;
	private TextView listError;
	private View detailsFrame;
	private String restoreSelection;

	protected abstract int getRefreshRate(); 
	protected abstract int getResourceViewId(); 
	protected abstract int getResourceItemId();

	protected abstract void updateDetails(View v, T item);
	protected abstract void updateItem(View v, T item);
	protected abstract List<T> fetchItems(SabControl sab) throws java.io.IOException;

	protected SabControl getSab()
	{
		return sab;
	}

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state)
	{
		Log.v(TAG, "onCreateView");
		this.inflater = inflater;

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
		Log.v(TAG, "onActivityCreated");
		super.onActivityCreated(state);

		SharedPreferences settings = getActivity().getPreferences(Activity.MODE_PRIVATE);
		restoreSelection = settings.getString(getClass().getName() + ":selected_id", null);
		if(restoreSelection != null)
			Log.v(TAG, "  restoreSelection = " + restoreSelection);
		else
			Log.v(TAG, "  restoreSelection = null");
	}

	@Override public void onHiddenChanged(boolean hidden)
	{
		super.onHiddenChanged(hidden);
		if(hidden)
		{
			Log.v(TAG, "onHidden(" + getClass().getName() + "), pausing");
			paused = true;
		}
		else
		{
			Log.v(TAG, "onHidden(" + getClass().getName() + "), resuming");
			paused = false;
			handler.post(updateListTask);
		}
	}

	@Override public void onPause()
	{
		super.onPause();
		Log.v(TAG, "onPause(" + getClass().getName() + ")");
		paused = true;

		SharedPreferences settings = getActivity().getPreferences(Activity.MODE_PRIVATE);
		T item = getCurrentItem();
		if(item != null)
			settings.edit().putString(getClass().getName() + ":selected_id", item.getId()).commit();
		else
			settings.edit().putString(getClass().getName() + ":selected_id", null).commit();
	}

	@Override public void onResume()
	{
		super.onResume();
		Log.v(TAG, "onResume(" + getClass().getName() + ")");

		//if we get a resume (like a return from the settings activity, but we are hidden, we want to stay
		//	paused
		if(!isHidden())
		{
			paused = false;
			handler.post(updateListTask);
			updateDetailsInternal(getCurrentItem());
		}
	}

	@Override public void onDestroy()
	{
		super.onDestroy();
		Log.v(TAG, "onDestroy(" + getClass().getName() + ")");
		handler.removeCallbacks(updateListTask);
	}

	public void reload()
	{
		handler.post(updateListTask);
	}

	protected T getCurrentItem()
	{
		int index = listView.getCheckedItemPosition();

		//did we hit zero items in the list while trying to update?
		if(listAdapter.getCount() == 0)
		{
			listSelectIndex(index);
			return null;
		}

		//if we are past the end of the list then unselect
		if(index >= listAdapter.getCount())
		{
			listSelectIndex(index);
			return null;
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
		listNoItems = (TextView)v.findViewById(R.id.list_noitems);
		listError = (TextView)v.findViewById(R.id.list_error);

		listProgress.setVisibility(View.INVISIBLE);
		listNoItems.setVisibility(View.VISIBLE);
		listError.setVisibility(View.INVISIBLE);

		listView.setAdapter(listAdapter);
		listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public synchronized void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
				T item = (T)parent.getAdapter().getItem(pos);
				updateDetailsInternal(item);
			}
		});

		initList(listView);
	}

	private void listUnselectAll()
	{
		for(int i = 0; i < listAdapter.getCount(); i++)
			listView.setItemChecked(i, false);
	}

	private void listSelectIndex(int index)
	{
		listUnselectAll();

		if(index >= 0 && index < listAdapter.getCount())
		{
			listView.setItemChecked(index, true);
			if(index < listView.getFirstVisiblePosition() || index > listView.getLastVisiblePosition())
				listView.smoothScrollToPositionFromTop(index, 100);

			updateDetailsInternal((T)listAdapter.getItem(index));
		}
		else
		{
			updateDetailsInternal(null);
		}
	}

	private void listSelectId(String id)
	{
		//if we don't find the id, then don't select anything
		int index = -1;

		for(int i = 0; i < listAdapter.getCount(); i++)
		{
			T item =  (T)listAdapter.getItem(i);

			//Log.v(TAG, " checking " + ((SabEntity)item).getId());
			if(item != null && (item instanceof SabEntity) && ((SabEntity)item).getId().equals(id))
			{
				index = i;
				break;
			}
		}

		//Log.v(TAG, "got index " + index);
		listSelectIndex(index);
	}

	protected void initList(ListView lv)
	{
	}

	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
	{
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
		T item = (T)listView.getItemAtPosition(info.position);
		onContextMenuForItem(menu, item);
	}

	protected void onContextMenuForItem(ContextMenu menu, T item)
	{
	}

	protected void setupDetails(View v)
	{
	}

	private void updateDetailsInternal(T item)
	{
		if(paused)
			return;

		//if nothing is selected then hide the details
		if(item == null)
		{
			detailsFrame.setVisibility(View.INVISIBLE);
			return;
		}

		updateDetails(detailsFrame, item);
		detailsFrame.setVisibility(View.VISIBLE);
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

	private Runnable updateSelectedIdTask = new Runnable() {
		public void run() {
			if(restoreSelection != null)
			{
				Log.v(TAG, "restoring selection of " + restoreSelection);
				listSelectId(restoreSelection);
				restoreSelection = null;
			}
		}
	};

	private class ItemAdapter extends ArrayAdapter<T>
	{
		public ItemAdapter(Context context, List<T> items)
		{
			super(context, getResourceItemId(), items);
		}

		@Override public View getView(int position, View convertView, ViewGroup parent)
		{
			View row;
			T item = getItem(position);

			if(null == convertView)
				row = inflater.inflate(getResourceItemId(), null);
			else
				row = convertView;

			updateItem(row, item);
			return row;
		}
	}

	private class UpdateListTask extends AsyncTask<Void, Void, List<T>>
	{
		protected List<T> doInBackground(Void... unused)
		{
			try
			{
				return fetchItems(sab);
			}
			catch(java.io.IOException e)
			{
				return null;
			}
		}

		protected void onPreExecute()
		{
			listProgress.setVisibility(View.VISIBLE);
			listNoItems.setVisibility(View.INVISIBLE);
			listError.setVisibility(View.INVISIBLE);
		}

		protected void onPostExecute(List<T> result)
		{
			if(result != null)
			{
				T item = getCurrentItem();

				//store the items
				items = new ArrayList<T>(result);

				//reload the list
				listAdapter.clear();
				listAdapter.addAll(result);
				listAdapter.notifyDataSetChanged();

				if(restoreSelection != null)
				{
					handler.postDelayed(updateSelectedIdTask, 10);
				}
				else if(item != null) //update details if we have a selected item
				{
					restoreSelection = item.getId();
					handler.postDelayed(updateSelectedIdTask, 10);
				}

				listError.setVisibility(View.INVISIBLE);

				//no items
				if(listAdapter.getCount() == 0)
					listNoItems.setVisibility(View.VISIBLE);
				else
					listNoItems.setVisibility(View.INVISIBLE);
			}
			else
			{
				//only display the error if we don't have any results
				if(listAdapter.getCount() == 0)
				{
					listNoItems.setVisibility(View.INVISIBLE);
					listError.setVisibility(View.VISIBLE);
				}
			}

			//hide the spinner
			listProgress.setVisibility(View.INVISIBLE);
		}
	}
}
