package cam.sabtab.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;

public final class Helper
{
	public static String formatSize(long size)
	{
		long limit = 10 * 1024;
		long limit2 = limit * 2 - 1;

		if(size < limit)
			return String.valueOf(size) + " bytes";
		else
		{
			size >>= 9;
			if(size < limit2)
				return String.valueOf((size + 1) / 2) + " kB";
			else
			{
				size >>= 10;
				if(size < limit2)
					return String.valueOf((size + 1) / 2) + " MB";
				else
				{
					size >>= 10;
					if(size < limit2)
						return String.valueOf((size + 1) / 2) + " GB";
					else
					{
						size >>= 10;
						return String.valueOf((size + 1) / 2) + " TB";
					}
				}
			}
		}
	}
}
