package cam.sabtab;
import cam.sabtab.model.Queue;
import cam.sabtab.model.QueueItem;
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
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;

public class SabTabActivity extends Activity
{
	private static final String TAG = "SabTabActivity";
	private boolean paused = false;
	private MenuItem pauseMenu;
	private MenuItem resumeMenu;
	private MenuItem statusMenu;
	private MenuItem speedMenu;
	private SabControl sab;
	private Queue queue;
	private Handler handler = new Handler(); 

	@Override public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		//create the activity view
		setContentView(R.layout.main);

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
		if(savedInstanceState != null) loadState(savedInstanceState);
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
		bar.selectTab(bar.getTabAt(tabSelected));
	}

	private void initTabs()
	{
		ActionBar bar = getActionBar();
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		Fragment queue = new TabQueueFragment();
		bar.addTab(bar.newTab().setText(R.string.tab_queue).setTabListener(new TabListener(queue)));

		Fragment history = new TabHistoryFragment();
		bar.addTab(bar.newTab().setText(R.string.tab_history).setTabListener(new TabListener(history)));

		Fragment status = new TabStatusFragment();
		bar.addTab(bar.newTab().setText(R.string.tab_status).setTabListener(new TabListener(status)));

		Fragment settings = new TabSettingsFragment();
		bar.addTab(bar.newTab().setText(R.string.tab_settings).setTabListener(new TabListener(settings)));
	}

	@Override public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);

		pauseMenu = menu.findItem(R.id.menu_pause);
		resumeMenu = menu.findItem(R.id.menu_resume);
		statusMenu = menu.findItem(R.id.menu_status);
		speedMenu = menu.findItem(R.id.menu_speed);

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

		updateMenus();
		return true;
	}

	@Override public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);

		ActionBar bar = getActionBar();
		int index = bar.getSelectedTab().getPosition();
		outState.putInt("tab_selected", index);
	}

	private void updateMenus()
	{
		ActionBar bar = getActionBar();

		if(queue != null)
		{
			if(queue.isPaused())
				statusMenu.setTitle(getString(R.string.header_paused));
			else if(queue.isIdle())
				statusMenu.setTitle(getString(R.string.header_idle));
			else
				statusMenu.setTitle(getString(R.string.header_running));

			if(queue.isRunning())
			{
				speedMenu.setVisible(true);
				speedMenu.setTitle(queue.getDownloadSpeed());
			}
			else
			{
				speedMenu.setVisible(false);
			}

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
			statusMenu.setTitle(getString(R.string.header_connecting));
			speedMenu.setVisible(false);
			resumeMenu.setVisible(false);
			pauseMenu.setVisible(true);
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
			Log.v(TAG, "downloadQueue");
			return sab.fetchQueue(0, 1);
		}

		protected void onPreExecute()
		{
		}

		protected void onPostExecute(Queue result)
		{
			if(result != null)
			{
				queue = result;
				updateMenus();
			}
		}
	}

	private class TabListener implements ActionBar.TabListener
	{
		private Fragment mFragment;

		public TabListener(Fragment fragment)
		{
			mFragment = fragment;
		}

		public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft)
		{
			ft.add(R.id.fragment_content, mFragment, null);
		}

		public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft)
		{
			ft.remove(mFragment);
		}

		public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft)
		{
		}
	}
}
