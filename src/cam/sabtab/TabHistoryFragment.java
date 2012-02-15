package cam.sabtab;
import cam.sabtab.model.SabControl;
import cam.sabtab.model.HistoryItem;
import cam.sabtab.ListDetailsFragment;

import java.util.List;

import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.RelativeLayout;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.util.Log;

public class TabHistoryFragment extends ListDetailsFragment<HistoryItem>
{
	private static final String TAG = "TabHistoryFragment";

	protected int getRefreshRate() { return 30000; }
	protected int getResourceViewId() { return R.layout.tab_history; }
	protected int getResourceItemId() { return R.layout.history_item; }

	protected void initList(ListView lv)
	{
		//register for the listview context menu
		registerForContextMenu(lv);
	}

	protected List<HistoryItem> fetchItems(SabControl sab)
	{
		Log.v(TAG, "fetch history items");
		return sab.fetchHistory(0, 100);
	}

	protected void updateItem(View row, HistoryItem item)
	{
		TextView tv = (TextView)row.findViewById(R.id.history_name);
		tv.setText(item.getName());

		tv = (TextView)row.findViewById(R.id.history_status);
		tv.setText(item.getStatus());

		tv = (TextView)row.findViewById(R.id.history_stats);
		tv.setText(item.getStats());
	}

	protected void updateDetails(View v, HistoryItem item)
	{
		TextView nzbid = (TextView)v.findViewById(R.id.details_id);
		TextView nzbname = (TextView)v.findViewById(R.id.details_name);
		TextView status = (TextView)v.findViewById(R.id.details_status);
		TextView size = (TextView)v.findViewById(R.id.details_size);
		TextView completed = (TextView)v.findViewById(R.id.details_completed);
		RelativeLayout details = (RelativeLayout)v.findViewById(R.id.details_list);

		nzbid.setText(item.getId());
		nzbname.setText(item.getName());
		status.setText(item.getStatus());
		size.setText(item.getSize());
		completed.setText(item.getCompleted());

		details.removeAllViews();
		int id = 1;

		if(item.getStatus().equals("Failed"))
		{
			String msg = item.getFailedMessage();

			//we don't need the link
			int index = msg.indexOf(", <a href");
			if(index != -1)
				msg = msg.substring(0, index);

			id = addDetailsSection(details, "Error", msg, id);
		}
		else
		{
			for(int i = 0; i < item.getStageNames().size(); i++)
			{
				String name = item.getStageNames().get(i);
				String action = item.getStageActions().get(i);

				if(name.equals("Script"))
					action = item.getScriptLog();

				id = addDetailsSection(details, name, action, id);
			}
		}
	}

	private int addDetailsSection(RelativeLayout details, String title, String text, int id)
	{
		RelativeLayout.LayoutParams params1 = new RelativeLayout.LayoutParams(
			RelativeLayout.LayoutParams.FILL_PARENT,
			RelativeLayout.LayoutParams.WRAP_CONTENT);
		params1.addRule(RelativeLayout.BELOW, id - 1);

		final TextView r1 = new TextView(getActivity());
		r1.setLayoutParams(params1);
		r1.setText(title);
		r1.setId(id++);
		r1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
		r1.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
		r1.setPadding(0, 10, 0, 0);
		details.addView(r1);

		RelativeLayout.LayoutParams params2 = new RelativeLayout.LayoutParams(
			RelativeLayout.LayoutParams.FILL_PARENT,
			RelativeLayout.LayoutParams.WRAP_CONTENT);
		params2.addRule(RelativeLayout.BELOW, id - 1);

		final TextView r2 = new TextView(getActivity());
		r2.setText(text);
		r2.setId(id++);
		r2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
		r2.setLayoutParams(params2);
		r2.setPadding(8, 4, 0, 0);
		details.addView(r2);

		return id;
	}

	@Override protected void onContextMenuForItem(ContextMenu menu, HistoryItem item)
	{
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
}
