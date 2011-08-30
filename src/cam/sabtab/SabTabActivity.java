package cam.sabtab;
import cam.sabtab.model.Queue;
import cam.sabtab.model.QueueItem;
import cam.sabtab.model.SabControl;
import cam.sabtab.model.SabControlEvent;

import java.util.List;

import android.app.Dialog;
import android.app.AlertDialog;
import android.app.Activity;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.TextView;
import android.util.Log;

public class SabTabActivity extends Activity
{
	private static final String TAG = "SabTabActivity";
	private boolean paused = false;
	private MenuItem pauseMenu;
	private MenuItem resumeMenu;
	private MenuItem speedMenu;
	private TextView statusText;
	private TextView remainsText;
	private TextView timeleftText;
	private SabControl sab;
	private Queue queue;
	private Handler handler = new Handler(); 

	@Override public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		Log.v(TAG, "onCreate()");

		FragmentManager fragmentManager = getFragmentManager();
		fragmentManager.enableDebugLogging(true);

		//create the activity view
		setContentView(R.layout.main);
		statusText = (TextView)findViewById(R.id.status);
		remainsText = (TextView)findViewById(R.id.remaining);
		timeleftText = (TextView)findViewById(R.id.time_left);

		//setup the sab controller with a refresher
		this.sab = new SabControl(getApplicationContext(), new SabControlEvent() {
			public void refresh() {
				handler.removeCallbacks(updateQueueTask);
				handler.post(updateQueueTask);
			}
      });

		//start the update timer
		handler.post(updateQueueTask);

		initTabs();
		if(savedInstanceState != null)
			loadState(savedInstanceState);

		//did we start with an intent to add an nzb?
		Intent intent = getIntent();
		if(intent.getAction().equals(Intent.ACTION_VIEW))
		{
			Log.v(TAG, "ACTION_VIEW");
			Log.v(TAG, intent.getData().getPath());
			sab.uploadFile(intent.getData().getPath());
		}

		ActionBar actionBar = getActionBar();
		actionBar.setTitle("");
	}

	@Override protected void onPause()
	{
		super.onPause();
		Log.v(TAG, "onPause()");
		paused = true;
	}

	@Override protected void onResume()
	{
		super.onResume();
		Log.v(TAG, "onResume()");
		paused = false;

		//send an update right now
		handler.removeCallbacks(updateQueueTask);
		handler.post(updateQueueTask);
	}

	@Override protected void onDestroy()
	{
		super.onResume();
		Log.v(TAG, "onDestroy()");
		handler.removeCallbacks(updateQueueTask);
	}

	private void loadState(Bundle state)
	{
		ActionBar bar = getActionBar();

		int tabSelected = state.getInt("tab_selected");
		Log.v(TAG, "loadState(tab=" + tabSelected + ")");
		bar.selectTab(bar.getTabAt(tabSelected));
	}

	@Override public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);

		ActionBar bar = getActionBar();
		int index = bar.getSelectedTab().getPosition();
		outState.putInt("tab_selected", index);
		Log.v(TAG, "saveState(tab=" + index + ")");
	}

	private void initTabs()
	{
		Log.v(TAG, "initTabs()");
		ActionBar bar = getActionBar();
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		FragmentManager fragmentManager = getFragmentManager();

		Fragment queueTab = fragmentManager.findFragmentByTag("queue");
	 	Fragment historyTab = fragmentManager.findFragmentByTag("history");

		if(queueTab == null) { queueTab = new TabQueueFragment(); Log.v(TAG, "new queue tab"); }
		if(historyTab == null) { historyTab = new TabHistoryFragment(); Log.v(TAG, "new history tab"); }

		bar.addTab(bar.newTab().setText(R.string.tab_queue).
			setTabListener(new TabListener(queueTab, "queue")));
		bar.addTab(bar.newTab().setText(R.string.tab_history).
			setTabListener(new TabListener(historyTab, "history")));

		int tabIndex = bar.getSelectedTab().getPosition();

		FragmentTransaction ft = fragmentManager.beginTransaction();
		if(queueTab.isAdded() && tabIndex != 0) ft.hide(queueTab);
		if(historyTab.isAdded() && tabIndex != 1) ft.hide(historyTab);
		ft.commit();

		Log.v(TAG, "initTabs() done");
	}

	@Override public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);

		pauseMenu = menu.findItem(R.id.menu_pause);
		resumeMenu = menu.findItem(R.id.menu_resume);
		speedMenu = menu.findItem(R.id.menu_speed);

		MenuItem settingsMenu = menu.findItem(R.id.menu_settings);
		settingsMenu.setIntent(new Intent(this, SettingsActivity.class));

		MenuItem warningsMenu = menu.findItem(R.id.menu_warnings);
		warningsMenu.setIntent(new Intent(this, WarningsActivity.class));

		pauseMenu.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				public boolean onMenuItemClick(MenuItem item) {
					sab.pauseQueue();
					return true;
				}
		});

		resumeMenu.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				public boolean onMenuItemClick(MenuItem item) {
					sab.resumeQueue();
					return true;
				}
		});

		updateState();
		return true;
	}

	private void updateState()
	{
		ActionBar bar = getActionBar();

		if(sab.getLastError() != null && sab.getLastError().length() > 0)
		{
			statusText.setText(getString(R.string.status_error));
			remainsText.setVisibility(View.GONE);
			timeleftText.setVisibility(View.GONE);
			speedMenu.setVisible(false);
			resumeMenu.setVisible(false);
			pauseMenu.setVisible(false);
		}
		else if(queue != null)
		{
			timeleftText.setVisibility(View.VISIBLE);

			if(queue.isPaused())
			{
				timeleftText.setText("");
				statusText.setText(getString(R.string.status_paused));
			}
			else if(queue.isIdle())
			{
				timeleftText.setText("");
				statusText.setText(getString(R.string.status_idle));
			}
			else
			{
				timeleftText.setText(String.format(getString(R.string.queue_timeleft),
					queue.getTimeLeft(), queue.getDownloadSpeed()));
				statusText.setText(String.format(getString(R.string.status_running),
					queue.getDownloadSpeed()));
			}

			speedMenu.setVisible(true);
			if(queue.getSpeedLimit() == 0)
				speedMenu.setTitle(getString(R.string.speed_unlimited));
			else
				speedMenu.setTitle(queue.getSpeedLimitText());

			remainsText.setVisibility(View.VISIBLE);
			remainsText.setText(String.format(getString(R.string.queue_remains),
				queue.getSizeLeft(), queue.getSizeTotal()));

			if(queue.isPaused())
			{
				resumeMenu.setVisible(true);
				pauseMenu.setVisible(false);
			}
			else
			{
				resumeMenu.setVisible(false);
				pauseMenu.setVisible(true);
			}
		}
		else
		{
			statusText.setText(getString(R.string.status_connecting));
			remainsText.setVisibility(View.GONE);
			timeleftText.setVisibility(View.GONE);
			speedMenu.setVisible(false);
			resumeMenu.setVisible(false);
			pauseMenu.setVisible(false);
		}
	}

	public void onStatusClick(View v)
	{
		//show the error if we are in the error state
		if(sab.getLastError() != null && sab.getLastError().length() > 0)
		{
			Toast toast = Toast.makeText(getApplicationContext(), sab.getLastError(),
				Toast.LENGTH_LONG);
			toast.show();
		}
	}

	@Override public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case android.R.id.home:
			case R.id.about:
				showAboutDialog();
				return true;
			case R.id.menu_speed:
				showSpeedDialog();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private void showAboutDialog()
	{
		try
		{
			String sabVersion;
			String tabVersion = getPackageManager().getPackageInfo(getPackageName(),
				PackageManager.GET_META_DATA).versionName;

			if(queue == null)
				sabVersion = "N/A";
			else
				sabVersion = queue.getVersion();

			Dialog dialog = new Dialog(this);
			dialog.setContentView(R.layout.about);
			dialog.setTitle(getString(R.string.about_title));

			TextView ver1 = (TextView)dialog.findViewById(R.id.tab_version);
			ver1.setText(getString(R.string.about_tab_version) + " " + tabVersion);
			TextView ver2 = (TextView)dialog.findViewById(R.id.sab_version);
			ver2.setText(getString(R.string.about_sab_version) + " " + sabVersion);

			dialog.show();
		} catch(Exception e) {
		}
	}

	private void showSpeedDialog()
	{
		final CharSequence[] items = {
			getString(R.string.speed_unlimited),
			"1024 kB/sec", "768 kB/sec", "512 kB/sec",
			"256 kB/sec", "128 kB/sec", "64 kB/sec", "32 kB/sec", 
			getString(R.string.speed_custom) };
		final int[] speeds = { 
			0,
			1024, 768, 512,
			256, 128, 64, 32 };

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.speed_title));

		builder.setItems(items, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				if(item == items.length - 1)
					showCustomSpeedDialog();
				else
					sab.setDownloadLimit(speeds[item]);
			}
		});

		AlertDialog dialog = builder.create();
		dialog.show();
	}

	private void showCustomSpeedDialog()
	{
		Toast.makeText(this, "custom speed dialog", Toast.LENGTH_SHORT).show();
	}

	//start the download task and re-ping ourselves for continual updates
	private Runnable updateQueueTask = new Runnable() {
		public void run() {
			if(paused) return;

			new DownloadQueueTask().execute();

			handler.removeCallbacks(updateQueueTask);
			handler.postDelayed(this, 5000);
		}
	};

	private class DownloadQueueTask extends AsyncTask<Void, Void, Queue>
	{
		protected Queue doInBackground(Void... unused)
		{
			Log.v(TAG, "download sab status");
			return sab.fetchQueue(0, 1);
		}

		protected void onPreExecute()
		{
		}

		protected void onPostExecute(Queue result)
		{
			if(result != null)
				queue = result;

			updateState();
		}
	}

	private class TabListener implements ActionBar.TabListener
	{
		private Fragment mFragment;
		private String tag;

		public TabListener(Fragment fragment, String tag)
		{
			this.tag = tag;
			mFragment = fragment;
		}

		public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft)
		{
			Log.v(TAG, "onTabSelected(" + mFragment.getClass().getName() + ", " + tag + ")");
			if(!mFragment.isAdded())
			{
				Log.v(TAG, "  adding to transaction");
				ft.add(R.id.fragment_content, mFragment, tag);
			}
			else
			{
				ft.show(mFragment);
			}

			ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
		}

		public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft)
		{
			Log.v(TAG, "onTabUnselected(" + mFragment.getClass().getName() + ", " + tag + ")");
			ft.hide(mFragment);
		}

		public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft)
		{
			Log.v(TAG, "onTabReselected(" + mFragment.getClass().getName() + ", " + tag + ")");

			if(tag.equals("queue")) ((TabQueueFragment)mFragment).reload();
			if(tag.equals("history")) ((TabHistoryFragment)mFragment).reload();
		}
	}
}
