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

		url = "http://192.168.2.2/sab/api?apikey=106f7d01dda207eb7907b30edb124dc8";
		//url = "http://192.168.0.50:8080/sab/api?apikey=106f7d01dda207eb7907b30edb124dc8";
	}

	public Queue fetchQueue(int offset, int count)
	{
		Log.v(TAG, "fetchQueue");

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

			Log.v(TAG, "fetchQueue done");
			return ret;
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public List<HistoryItem> fetchHistory(int offset, int count)
	{
		Log.v(TAG, "fetchHistory");
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

			Log.v(TAG, "fetchHistory done");
			return ret;
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public List<WarningItem> fetchWarnings()
	{
		Log.v(TAG, "fetchWarning");
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
		Log.v(TAG, "resumeSlot(" + item.getId() + ")");

		AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
			protected Void doInBackground(Void... unused) {
				sendCommand("mode=queue&name=resume&value=" + item.getId());
				return null;
			}
      };

		task.execute();
	}
}
