package cam.sabtab;
import cam.sabtab.model.Queue;
import cam.sabtab.model.QueueItem;
import cam.sabtab.model.SabControl;
import cam.sabtab.model.SabControlEvent;

import java.util.List;

import android.app.Activity;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
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
	private TextView statusText;
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

		FragmentTransaction ft = fragmentManager.beginTransaction();
		if(queueTab.isAdded()) ft.hide(queueTab);
		if(historyTab.isAdded()) ft.hide(historyTab);
		ft.commit();

		Log.v(TAG, "initTabs() done");
	}

	@Override public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);

		pauseMenu = menu.findItem(R.id.menu_pause);
		resumeMenu = menu.findItem(R.id.menu_resume);

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
			resumeMenu.setVisible(false);
			pauseMenu.setVisible(false);
		}
		else if(queue != null)
		{
			if(queue.isPaused())
				statusText.setText(getString(R.string.status_paused));
			else if(queue.isIdle())
				statusText.setText(getString(R.string.status_idle));
			else
				statusText.setText(String.format(getString(R.string.status_running),
					queue.getDownloadSpeed()));

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
			resumeMenu.setVisible(false);
			pauseMenu.setVisible(false);
			statusText.setText(getString(R.string.status_connecting));
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
		}
	}
}
