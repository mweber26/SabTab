package cam.sabtab;

import android.app.Activity;
import android.app.ActionBar;
import android.content.SharedPreferences;
import android.preference.PreferenceActivity;
import android.preference.EditTextPreference;
import android.preference.PreferenceFragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

public class SettingsActivity extends PreferenceActivity
{
	@Override protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}
}
