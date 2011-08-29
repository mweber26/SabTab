package cam.sabtab.model;
import cam.sabtab.model.SabEntity;

import java.util.ArrayList;
import java.util.Date;
import java.text.DateFormat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class HistoryItem implements SabEntity
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
	private long bytes;
	private int postProcTime;
	private ArrayList<String> stageNames;
	private ArrayList<String> stageActions;

	public HistoryItem(JSONObject item) throws JSONException
	{
		id = item.getString("nzo_id");
		failMessage = item.getString("fail_message");
		script = item.getString("script");
		name = item.getString("name");
		script = item.getString("script");
		scriptLog = item.getString("script_log");
		status = item.getString("status");
		downloadTime = item.getInt("download_time");
		bytes = item.getLong("bytes");
		postProcTime = item.getInt("postproc_time");
		completedAt = new Date(item.getLong("completed") * 1000);

		stageNames = new ArrayList<String>();
		stageActions = new ArrayList<String>();
		JSONArray slotArray = item.getJSONArray("stage_log");
		for(int i = 0; i < slotArray.length(); i++)
		{
			JSONObject slot = slotArray.getJSONObject(i);
			JSONArray actionArray = slot.getJSONArray("actions");
			stageNames.add(slot.getString("name"));
			String action = "";
			for(int j = 0; j < actionArray.length(); j++)
				action = action + actionArray.getString(j) + "\r\n";
			stageActions.add(action.trim());
		}
	}

	public ArrayList<String> getStageNames() { return stageNames; }
	public ArrayList<String> getStageActions() { return stageActions; }

	public String getId() { return id; }
	public String getName() { return name; }
	public String getScriptLog() { return scriptLog; }
	public String getStatus() { return status; }
	public String getSize() { return Helper.formatSize(bytes); }
	public String getSpeed() { return Helper.formatSize(bytes / downloadTime) + "/sec"; }
	public String getCompleted() { return DateFormat.getDateTimeInstance().format(completedAt); }

	public String getTime()
	{
		String ret = "";
		long sec = downloadTime;
		long diff[] = new long[] { 0, 0, 0, 0 };

		diff[3] = (sec >= 60 ? sec % 60 : sec);
		diff[2] = (sec = (sec / 60)) >= 60 ? sec % 60 : sec;
		diff[1] = (sec = (sec / 60)) >= 24 ? sec % 24 : sec;
		diff[0] = (sec = (sec / 24));

		if(diff[0] > 0)
		{
			if(ret.length() > 0) ret = ret + ", ";
			ret = ret + String.format("%d day%s", diff[0], diff[0] > 1 ? "s" : "");
		}
		if(diff[1] > 0)
		{
			if(ret.length() > 0) ret = ret + ", ";
			ret = ret + String.format("%d hour%s", diff[1], diff[1] > 1 ? "s" : "");
		}
		if(diff[2] > 0)
		{
			if(ret.length() > 0) ret = ret + ", ";
			ret = ret + String.format("%d min%s", diff[2], diff[2] > 1 ? "s" : "");
		}
		if(diff[3] > 0)
		{
			if(ret.length() > 0) ret = ret + ", ";
			ret = ret + String.format("%d sec%s", diff[3], diff[3] > 1 ? "s" : "");
		}

		return ret;
	}

	public String getStats()
	{
		return getSize() + " @ " + getSpeed();
	}
}
