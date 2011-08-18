package cam.sabtab.model;

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

public class HistoryItem
{
	private String id;
	private String name;
	private String failMessage;
	private String category;
	private String status;
	private int downloadTime;
	private String script;
	private String scriptLog;
	private Date completedAt;
	private Date downloadedAt;
	private long bytes;
	private int postProcTime;

	public HistoryItem(JSONObject slot) throws JSONException
	{
		id = slot.getString("nzo_id");
		failMessage = slot.getString("fail_message");
		script = slot.getString("script");
		name = slot.getString("name");
		script = slot.getString("script");
		scriptLog = slot.getString("script_log");
		status = slot.getString("status");
		downloadTime = slot.getInt("download_time");
		bytes = slot.getLong("bytes");
		postProcTime = slot.getInt("postproc_time");
		completedAt = new Date(slot.getLong("completed"));
		downloadedAt = new Date(slot.getLong("downloaded"));
	}

	public String getName() { return name; }
	public String getScriptLog() { return scriptLog; }
	public String getStatus() { return status; }

	public String getStats()
	{
		return Helper.formatSize(bytes) + " @ " + Helper.formatSize(bytes / downloadTime) + "/sec";
	}
}
