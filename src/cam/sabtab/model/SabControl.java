package cam.sabtab.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.File;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.text.ParseException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

public class SabControl
{
	private static final String TAG = "SabControl";
	private SharedPreferences prefs;
	private Context context;
	private SabControlEvent event;
	private String error;

	public SabControl(Context context, SabControlEvent event)
	{
		this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
		this.event = event;
	}

	public String getLastError()
	{
		return error;
	}

	private String getUrl()
	{
		//url = "http://192.168.2.2/sab/api?apikey=106f7d01dda207eb7907b30edb124dc8";
		//url = "http://192.168.0.50:8080/sab/api?apikey=106f7d01dda207eb7907b30edb124dc8";

		String url = prefs.getString("sabserver", "");
		if(url.length() == 0)
		{
			error = "Invalid SABnzbd server URL.  Please verify your settings are correct.";
			return null;
		}

		String apikey = prefs.getString("sabapikey", "");
		if(apikey.length() == 0)
		{
			error = "Invalid SABnzbd API key.  Please verify your settings are correct.";
			return null;
		}

		if(url.endsWith("/"))
			return url + "api?apikey=" + apikey;
		else
			return url + "/api?apikey=" + apikey;
	}

	private void processException(Exception e)
	{
		e.printStackTrace();

		if(e instanceof java.net.UnknownHostException)
			error = "Invalid SABnzbd hostname.  Please verify your settings are correct.";
		else if(e instanceof java.net.MalformedURLException)
			error = "Malformed SABnzbd URL.  Please verify your settings are correct.";
		else if(e instanceof java.lang.NullPointerException)
			error = "Malformed SABnzbd URL.  Please verify your settings are correct.";
		else if(e instanceof java.io.FileNotFoundException)
			error = "Invalid SABnzbd path.  Please verify your settings are correct.";
		else if(e instanceof java.net.ConnectException)
			error = "Cannot connect to SABnzbd host.  Please verify your settings are correct.";
		else
			error = e.getMessage();
	}

	private String makeRequest(String request, boolean isJson)
	{
		try
		{
			String baseUrl = getUrl();
			if(baseUrl == null) return null;

			String connUrl = baseUrl + "&" + request;
			URL src = new URL(connUrl);

			URLConnection tc = src.openConnection();
			BufferedReader in = new BufferedReader(new InputStreamReader(tc.getInputStream()));

			String json = "", line;
			while((line = in.readLine()) != null)
				json = json + line;

			if(json.trim().equals("error: API Key Incorrect"))
			{
				error = "Invalid SABnzbd API key.  Please verify your settings are correct.";
				return null;
			}

			if(isJson)
			{
				JSONObject ret = new JSONObject(json);
				String retErr = ret.optString("error");

				if(retErr != null && retErr.length() > 0)
				{
			 		if(retErr.equals("API Key Incorrect"))
						error = "Invalid SABnzbd API key.  Please verify your settings are correct.";
					else
						error = retErr;
	
					return null;
				}
			}

			//successful, so far so clear the error
			error = null;

			return json;
		} catch(Exception e) {
			processException(e);
			return null;
		}
	}

	public Queue fetchQueue(int offset, int count)
	{
		try
		{
			String json = makeRequest("start="+offset+"&limit="+count+"&mode=queue&output=json",
				true);
			if(json == null) return null;

			JSONObject queue = new JSONObject(json).getJSONObject("queue");
			Queue ret = new Queue(queue);
			return ret;
		} catch(JSONException e) {
			error = "Error connecting to SABnzbd, cannot process queue download.";
			e.printStackTrace();
			return null;
		}
	}

	public List<HistoryItem> fetchHistory(int offset, int count)
	{
		ArrayList<HistoryItem> ret = new ArrayList<HistoryItem>();

		try
		{
			String json = makeRequest("start="+offset+"&limit="+count+"&mode=history&output=json",
				true);
			if(json == null) return null;

			JSONObject history = new JSONObject(json).getJSONObject("history");
			JSONArray slots = history.getJSONArray("slots");

			for(int i = 0; i < slots.length(); i++)
			{
				JSONObject slot = slots.getJSONObject(i);
				ret.add(new HistoryItem(slot));
			}

			return ret;
		} catch(JSONException e) {
			error = "Error connecting to SABnzbd, cannot process history download.";
			e.printStackTrace();
			return null;
		}
	}

	public List<WarningItem> fetchWarnings()
	{
		ArrayList<WarningItem> ret = new ArrayList<WarningItem>();

		try
		{
			String json = makeRequest("mode=warnings&output=json", true);
			if(json == null) return null;

			JSONArray slots = new JSONObject(json).getJSONArray("warnings");
			for(int i = 0; i < slots.length(); i++)
				ret.add(new WarningItem(slots.getString(i)));

			Collections.reverse(ret);
			return ret;
		} catch(JSONException e) {
			error = "Error connecting to SABnzbd, cannot process warnings download.";
			e.printStackTrace();
			return null;
		} catch(ParseException e) {
			error = "Error connecting to SABnzbd, cannot process warnings download.";
			e.printStackTrace();
			return null;
		}
	}

	private boolean sendCommand(String cmd)
	{
		String ret = makeRequest(cmd, false);
		if(ret == null) return false;

		Log.v(TAG, "ret = " + ret);
		if(event != null) event.refresh();

		return true;
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

	public void uploadFile(final String file)
	{
		Log.v(TAG, "uploadFile(" + file + ")");

		AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
			protected Void doInBackground(Void... unused) {
				String baseUrl = getUrl();
				if(baseUrl == null) return null;

				postFile(baseUrl + "&mode=addfile", file);
				return null;
			}
      };

		task.execute();
	}

	private boolean postFile(String urlServer, String filepath)
	{
		HttpURLConnection connection = null;
		DataOutputStream outputStream = null;
		DataInputStream inputStream = null;

		String lineEnd = "\r\n";
		String twoHyphens = "--";
		String boundary =  "*****";
		boolean ret = false;

		int bytesRead, bytesAvailable, bufferSize;
		byte[] buffer;
		int maxBufferSize = 1*1024*1024;

		try
		{
			File file = new File(filepath);
			FileInputStream fileInputStream = new FileInputStream(file);

			URL url = new URL(urlServer);
			connection = (HttpURLConnection)url.openConnection();

			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setUseCaches(false);

			connection.setRequestMethod("POST");

			connection.setRequestProperty("Connection", "Keep-Alive");
			connection.setRequestProperty("Content-Type", "multipart/form-data;boundary="+boundary);

			outputStream = new DataOutputStream( connection.getOutputStream() );
			outputStream.writeBytes(twoHyphens + boundary + lineEnd);
			outputStream.writeBytes("Content-Disposition: form-data;" +
				" name=\"nzbfile\";filename=\"" + file.getName() +"\"" + lineEnd);
			outputStream.writeBytes(lineEnd);

			bytesAvailable = fileInputStream.available();
			bufferSize = Math.min(bytesAvailable, maxBufferSize);
			buffer = new byte[bufferSize];

			bytesRead = fileInputStream.read(buffer, 0, bufferSize);
			while(bytesRead > 0)
			{
				outputStream.write(buffer, 0, bufferSize);
				bytesAvailable = fileInputStream.available();
				bufferSize = Math.min(bytesAvailable, maxBufferSize);
				bytesRead = fileInputStream.read(buffer, 0, bufferSize);
			}

			outputStream.writeBytes(lineEnd);
			outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

			//serverResponseCode = connection.getResponseCode();
			String serverResponseMessage = connection.getResponseMessage();
			Log.v(TAG, "response = " + serverResponseMessage);
			if(serverResponseMessage.toLowerCase().equals("ok"))
			{
				ret = true;

				if(event != null) event.refresh();
			}
			else
			{
				error = serverResponseMessage;
				ret = false;
			}

			fileInputStream.close();
			outputStream.flush();
			outputStream.close();

			return ret;
		}
		catch(Exception e) {
			processException(e);
			return false;
		}
	}
}
