package cam.sabtab;
import cam.sabtab.model.SabControl;
import cam.sabtab.model.HistoryItem;
import cam.sabtab.ListDetailsFragment;

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

public class TabHistoryFragment extends ListDetailsFragment<HistoryItem>
{
	private static final String TAG = "TabHistoryFragment";

	protected int getRefreshRate() { return 30000; }
	protected int getResourceViewId() { return R.layout.tab_history; }
	protected int getResourceItemId() { return R.layout.history_item; }

	@Override protected void initList(ListView lv)
	{
		//register for the listview context menu
		registerForContextMenu(lv);
	}

	protected List<HistoryItem> fetchItems(SabControl sab)
	{
		Log.v(TAG, "fetch history items");
		return sab.fetchHistory(0, 50);
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

	protected void setupDetails(View v)
	{
	}

	protected void updateDetails(View v, HistoryItem item)
	{
		TextView detailsScriptLog = (TextView)v.findViewById(R.id.detail_script_log);
		detailsScriptLog.setText(item.getScriptLog());
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
