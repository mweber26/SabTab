package cam.sabtab;
import cam.sabtab.model.SabControl;
import cam.sabtab.model.Queue;
import cam.sabtab.model.QueueItem;
import cam.sabtab.ListDetailsFragment;

import java.util.ArrayList;
import java.util.List;

import android.view.View;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.util.Log;

public class TabQueueFragment extends ListDetailsFragment<QueueItem>
{
	private static final String TAG = "TabQueueFragment";

	protected int getRefreshRate() { return 10000; }
	protected int getResourceViewId() { return R.layout.tab_queue; }
	protected int getResourceItemId() { return R.layout.queue_item; }

	protected void initList(ListView lv)
	{
		//register for the listview context menu
		registerForContextMenu(lv);
	}

	protected List<QueueItem> fetchItems(SabControl sab)
	{
		Log.v(TAG, "fetch queue items");
		Queue queue = sab.fetchQueue(0, 50);
		if(queue != null)
			return queue.getSlots();
		else
			return null;
	}

	protected void updateItem(View row, QueueItem item)
	{
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
	}

	@Override protected void onContextMenuForItem(ContextMenu menu, final QueueItem item)
	{
		menu.setHeaderTitle(item.getName());

		if(item.isPaused())
		{
			MenuItem resume = menu.add(R.string.queue_list_resume);
			resume.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
					public boolean onMenuItemClick(MenuItem menu) {
						getSab().resumeSlot(item);
						return true;
					}
			});
		}
		else
		{
			MenuItem pause = menu.add(R.string.queue_list_pause);
			pause.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
					public boolean onMenuItemClick(MenuItem menu) {
						getSab().pauseSlot(item);
						return true;
					}
			});
		}

		MenuItem moveUp = menu.add(R.string.queue_list_moveup);
		moveUp.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				public boolean onMenuItemClick(MenuItem menu) {
					return true;
				}
		});

		MenuItem moveDown = menu.add(R.string.queue_list_movedown);
		moveDown.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				public boolean onMenuItemClick(MenuItem menu) {
					return true;
				}
		});

		MenuItem cancel = menu.add(R.string.queue_list_cancel);
		cancel.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				public boolean onMenuItemClick(MenuItem menu) {
					return true;
				}
		});
	}

	protected void updateDetails(View v, QueueItem item)
	{
		TextView id = (TextView)v.findViewById(R.id.details_id);
		TextView name = (TextView)v.findViewById(R.id.details_name);
		TextView status = (TextView)v.findViewById(R.id.details_status);
		TextView time = (TextView)v.findViewById(R.id.details_time);
		TextView eta = (TextView)v.findViewById(R.id.details_eta);
		TextView left = (TextView)v.findViewById(R.id.details_left);
		TextView total = (TextView)v.findViewById(R.id.details_total);
		TextView age = (TextView)v.findViewById(R.id.details_age);

		id.setText(item.getId());
		name.setText(item.getName());
		status.setText(item.getStatus());
		time.setText(item.getTimeLeft());
		eta.setText(item.getEta());
		left.setText(item.getSizeLeft());
		total.setText(item.getSizeTotal());
		age.setText(item.getAge());

		loadPrioritySpinner(v, item);
		loadCategorySpinner(v, item);
		loadUnpackSpinner(v, item);
		loadScriptSpinner(v, item);
	}

	private void loadPrioritySpinner(View v, final QueueItem item)
	{
		ArrayList<String> arr = new ArrayList<String>();
		arr.add(getString(R.string.priority_force));
		arr.add(getString(R.string.priority_high));
		arr.add(getString(R.string.priority_normal));
		arr.add(getString(R.string.priority_low));

		Spinner spin = (Spinner)v.findViewById(R.id.queue_priority);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity().getApplicationContext(),
			android.R.layout.simple_spinner_item, arr);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		spin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> arg0, View view, int position, long id) {
				getSab().changePriority(item, position);
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

	private void loadCategorySpinner(View v, final QueueItem item)
	{
		final ArrayList<String> cats = item.getQueue().getCategories();

		Spinner spin = (Spinner)v.findViewById(R.id.queue_category);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity().getApplicationContext(),
			android.R.layout.simple_spinner_item, cats);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		spin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> arg0, View view, int position, long id) {
				getSab().changeCategory(item, "Default", cats.get(position));
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

	private void loadUnpackSpinner(View v, final QueueItem item)
	{
		ArrayList<String> arr = new ArrayList<String>();
		arr.add(getString(R.string.unpack1));
		arr.add(getString(R.string.unpack2));
		arr.add(getString(R.string.unpack3));
		arr.add(getString(R.string.unpack4));

		Spinner spin = (Spinner)v.findViewById(R.id.queue_unpack);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity().getApplicationContext(),
			android.R.layout.simple_spinner_item, arr);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		spin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> arg0, View view, int position, long id) {
				getSab().changeUnpack(item, position);
			}

			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});

		spin.setAdapter(adapter);
		spin.setSelection(item.getUnpackOptionIndex());
	}

	private void loadScriptSpinner(View v, final QueueItem item)
	{
		final ArrayList<String> scripts = item.getQueue().getScripts();

		Spinner spin = (Spinner)v.findViewById(R.id.queue_script);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity().getApplicationContext(),
			android.R.layout.simple_spinner_item, scripts);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		spin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> arg0, View view, int position, long id) {
				getSab().changeScript(item, scripts.get(position));
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
}
