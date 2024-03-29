package cam.sabtab.model;

import java.util.List;
import java.util.ArrayList;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class Queue
{
	private String status;
	private String totalMB;
	private String leftMB;
	private String timeLeft;
	private String version;
	private int speedLimit;
	private int currentSpeed;
	private int numSlots;
	private ArrayList<QueueItem> slots;
	private ArrayList<String> categories;
	private ArrayList<String> scripts;

	public Queue(JSONObject queue) throws JSONException
	{
		status = queue.getString("status");
		timeLeft = queue.getString("timeleft");
		version = queue.getString("version");
		totalMB = "" + queue.getDouble("mb");
		leftMB = "" + queue.getDouble("mbleft");
		numSlots = queue.getInt("noofslots");
		currentSpeed = queue.getInt("kbpersec") * 1024;

		if(queue.getString("speedlimit") == null)
			speedLimit = 0;
		else if(queue.getString("speedlimit").length() == 0)
			speedLimit = 0;
		else
			speedLimit = queue.getInt("speedlimit");

		slots = new ArrayList<QueueItem>();
		JSONArray slotArray = queue.getJSONArray("slots");
		for(int i = 0; i < slotArray.length(); i++)
		{
			JSONObject slot = slotArray.getJSONObject(i);
			slots.add(new QueueItem(this, slot));
		}

		categories = new ArrayList<String>();
		JSONArray catArray = queue.getJSONArray("categories");
		for(int i = 0; i < catArray.length(); i++)
		{
			String cat = catArray.getString(i);

			if(cat.equals("*"))
				categories.add("Default");
			else
				categories.add(cat);
		}

		scripts = new ArrayList<String>();
		JSONArray scriptArray = queue.getJSONArray("scripts");
		for(int i = 0; i < scriptArray.length(); i++)
			scripts.add(scriptArray.getString(i));
	}

	public int getNumberOfSlots() { return numSlots; }
	public ArrayList<QueueItem> getSlots() { return slots; }
	public String getStatus() { return status; }
	public String getVersion() { return version; }
	public int getSpeedLimit() { return speedLimit; }
	public String getSpeedLimitText() { return speedLimit + " kB/sec"; }

	public ArrayList<String> getScripts() { return scripts; }
	public ArrayList<String> getCategories() { return categories; }

	public String getDownloadSpeed() { return Helper.formatSize(currentSpeed) + "/sec"; }
	public String getSizeTotal() { return totalMB + " MB"; }
	public String getSizeLeft() { return leftMB + " MB"; }
	public String getTimeLeft() { return timeLeft; }

	public boolean isRunning() { return !status.equals("Paused"); }
	public boolean isPaused() { return status.equals("Paused"); }
	public boolean isIdle() { return numSlots == 0; }
}
