package cam.sabtab.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import android.util.Log;

public class WarningItem
{
	private String text;
	private String level;
	private Date date;

	public WarningItem(String line) throws ParseException
	{
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
		String[] vals = line.split("\n");

		this.date = formatter.parse(vals[0]);
		this.level = vals[1];
		this.text = vals[2];
	}

	public String getText() { return text; }
	public String getLevel() { return level; }
	public Date getDate() { return date; }
}
