package cam.sabtab.model;
import cam.sabtab.model.SabEntity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class QueueItem implements SabEntity
{
	private Queue queue;
	private String id;
	private String status;
	private String timeLeft;
	private String script;
	private double totalMB;
	private double leftMB;
	private String name;
	private String priority;
	private String category;
	private int percentage;
	private int unpackOptions;
	private String avgAge;
	private String eta;

	public QueueItem(Queue queue, JSONObject slot) throws JSONException
	{
		this.queue = queue;
		id = slot.getString("nzo_id");
		status = slot.getString("status");
		timeLeft = slot.getString("timeleft");
		script = slot.getString("script");
		totalMB = slot.getDouble("mb");
		leftMB = slot.getDouble("mbleft");
		name = slot.getString("filename");
		priority = slot.getString("priority");
		category = slot.getString("cat");
		percentage = slot.getInt("percentage");
		unpackOptions = slot.getInt("unpackopts");
		avgAge = slot.getString("avg_age");
		eta = slot.getString("eta");
	}

	public Queue getQueue() { return queue; }

	public String getName() { return name; }
	public String getStatus() { return status; }
	public String getStats()
	{
		return getPriority() + " | "
			+ getCategory() + " | "
			+ getSizeLeft() + " left of " + getSizeTotal();
	}

	public String getSizeTotal() { return Helper.formatSize((long)(totalMB * 1024 * 1024)); }
	public String getSizeLeft() { return Helper.formatSize((long)(leftMB * 1024 * 1024)); }
	public String getPriority() { return priority; }
	public String getCategory() { return category.equals("*") ? "Default" : category; }
	public String getTimeLeft() { return timeLeft; }
	public String getId() { return id; }
	public String getEta() { return eta; }
	public String getAge() { return avgAge; }
	public String getScript() { return script; }
	public int getProgress() { return percentage; }
	public int getUnpackOptionIndex() { return unpackOptions; }

	public boolean isPaused() { return status.equals("Paused"); }
	public boolean isDownloading() { return status.equals("Downloading"); }
}
