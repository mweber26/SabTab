package cam.sabtab.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.text.ParseException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

public class SabControl
{
	private static final String TAG = "SabControl";
	private Context context;
	private String url;
	private SabControlEvent event;

	public SabControl(Context context, SabControlEvent event)
	{
		this.event = event;

		//url = "http://192.168.2.2/sab/api?apikey=106f7d01dda207eb7907b30edb124dc8";
		url = "http://192.168.0.50:8080/sab/api?apikey=106f7d01dda207eb7907b30edb124dc8";
	}

	public Queue fetchQueue(int offset, int count)
	{
		try
		{
			String connUrl = url + "&start=" + offset + "&limit=" + count + "&mode=queue&output=json";
			URL src = new URL(connUrl);

			URLConnection tc = src.openConnection();
			BufferedReader in = new BufferedReader(new InputStreamReader(tc.getInputStream()));

			String json = "", line;
			while((line = in.readLine()) != null)
				json = json + line;

			JSONObject queue = new JSONObject(json).getJSONObject("queue");
			Queue ret = new Queue(queue);
			return ret;
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public List<HistoryItem> fetchHistory(int offset, int count)
	{
		ArrayList<HistoryItem> ret = new ArrayList<HistoryItem>();

		try
		{
			String connUrl = url + "&start=" + offset + "&limit=" + count + "&mode=history&output=json";
			URL src = new URL(connUrl);

			URLConnection tc = src.openConnection();
			BufferedReader in = new BufferedReader(new InputStreamReader(tc.getInputStream()));

			String json = "", line;
			while((line = in.readLine()) != null)
				json = json + line;

			JSONObject history = new JSONObject(json).getJSONObject("history");
			JSONArray slots = history.getJSONArray("slots");

			for(int i = 0; i < slots.length(); i++)
			{
				JSONObject slot = slots.getJSONObject(i);
				ret.add(new HistoryItem(slot));
			}

			return ret;
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public List<WarningItem> fetchWarnings()
	{
		ArrayList<WarningItem> ret = new ArrayList<WarningItem>();

		try
		{
			String connUrl = url + "&mode=warnings&output=json";
			URL src = new URL(connUrl);

			URLConnection tc = src.openConnection();
			BufferedReader in = new BufferedReader(new InputStreamReader(tc.getInputStream()));

			String json = "", line;
			while((line = in.readLine()) != null)
				json = json + line;

			JSONArray slots = new JSONObject(json).getJSONArray("warnings");
			for(int i = 0; i < slots.length(); i++)
				ret.add(new WarningItem(slots.getString(i)));

			Collections.reverse(ret);
			return ret;
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private boolean sendCommand(String cmd)
	{
		try
		{
			String connUrl = url + "&" + cmd;
			URL src = new URL(connUrl);

			URLConnection tc = src.openConnection();
			BufferedReader in = new BufferedReader(new InputStreamReader(tc.getInputStream()));
			String line = in.readLine();

			Log.v(TAG, "ret = " + line);
			if(event != null) event.refresh();

			return true;
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public void resumeQueue()
	{
		Log.v(TAG, "resumeQueue");

		AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
			protected Void doInBackground(Void... unused) {
				sendCommand("mode=resume");
				return null;
			}
      };

		task.execute();
	}

	public void pauseQueue()
	{
		Log.v(TAG, "pauseQueue");

		AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
			protected Void doInBackground(Void... unused) {
				sendCommand("mode=pause");
				return null;
			}
      };

		task.execute();
	}

	public void pauseSlot(final QueueItem item)
	{
		if(item == null) return;
		Log.v(TAG, "pauseSlot(" + item.getId() + ")");

		AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
			protected Void doInBackground(Void... unused) {
				sendCommand("mode=queue&name=pause&value=" + item.getId());
				return null;
			}
      };

		task.execute();
	}

	public void resumeSlot(final QueueItem item)
	{
		if(item == null) return;
		Log.v(TAG, "resumeSlot(" + item.getId() + ")");

		AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
			protected Void doInBackground(Void... unused) {
				sendCommand("mode=queue&name=resume&value=" + item.getId());
				return null;
			}
      };

		task.execute();
	}

	public void changePriority(final QueueItem item, int priority)
	{
		if(item == null) return;

		if(priority == 0 && item.getPriority().equals("Force")) return;
		if(priority == 1 && item.getPriority().equals("High")) return;
		if(priority == 2 && item.getPriority().equals("Normal")) return;
		if(priority == 3 && item.getPriority().equals("Low")) return;

		Log.v(TAG, "changePriority(" + item.getId() + ", " + priority + ")");

		if(priority == 0) //force
			priority = 2;
		if(priority == 1) //high
			priority = 1;
		if(priority == 2) //normal
			priority = 0;
		if(priority == 3) //low
			priority = -1;

		final int taskPriority = priority;
		AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
			protected Void doInBackground(Void... unused) {
				sendCommand("mode=queue&name=priority&value=" + item.getId() + "&value2=" + taskPriority);
				return null;
			}
      };

		task.execute();
	}

	public void changeCategory(final QueueItem item, String defaultCategoryName, String category)
	{
		if(item == null) return;

		if(!item.getCategory().equals(category))
		{
			Log.v(TAG, "changeCategory(" + item.getId() + ", " + category + ")");

			if(defaultCategoryName.equals(category))
				category = "*";

			final String taskCategory = category;
			AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
				protected Void doInBackground(Void... unused) {
					sendCommand("mode=change_cat&value=" + item.getId() + "&value2=" + taskCategory);
					return null;
				}
      	};

			task.execute();
		}
	}

	public void changeScript(final QueueItem item, final String script)
	{
		if(item == null) return;

		if(!item.getScript().equals(script))
		{
			Log.v(TAG, "changeScript(" + item.getId() + ", " + script + ")");

			AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
				protected Void doInBackground(Void... unused) {
					sendCommand("mode=change_script&value=" + item.getId() + "&value2=" + script);
					return null;
				}
      	};

			task.execute();
		}
	}

	public void changeUnpack(final QueueItem item, final int unpack)
	{
		if(item == null) return;

		if(item.getUnpackOptionIndex() != unpack)
		{
			Log.v(TAG, "changeUnpack(" + item.getId() + ", " + unpack + ")");

			AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
				protected Void doInBackground(Void... unused) {
					sendCommand("mode=change_opts&value="+item.getId()+"&value2="+unpack);
					return null;
				}
      	};

			task.execute();
		}
	}

	public void moveItem(final QueueItem item, final int slot)
	{
		if(item == null) return;

		if(item.getSlotIndex() != slot)
		{
			Log.v(TAG, "moveItem(" + item.getId() + ", " + slot + ")");

			AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
				protected Void doInBackground(Void... unused) {
					sendCommand("mode=switch&value=" + item.getId() + "&value2=" + slot);
					return null;
				}
      	};

			task.execute();
		}
	}

	public void deleteItem(final QueueItem item)
	{
		if(item == null) return;

		Log.v(TAG, "deleteItem(" + item.getId() + ")");

		AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
			protected Void doInBackground(Void... unused) {
				sendCommand("mode=queue&name=delete&value=" + item.getId());
				return null;
			}
      };

		task.execute();
	}
}
