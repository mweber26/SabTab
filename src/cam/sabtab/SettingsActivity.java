package cam.sabtab;

import android.app.Activity;
import android.app.ActionBar;
import android.content.SharedPreferences;
import android.preference.PreferenceActivity;
import android.preference.EditTextPreference;
import android.preference.PreferenceFragment;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.util.Log;

public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener
{
	private EditTextPreference sabserver;
	private EditTextPreference sabapikey;

	@Override protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		sabserver = (EditTextPreference)getPreferenceScreen().findPreference("sabserver");
		sabapikey = (EditTextPreference)getPreferenceScreen().findPreference("sabapikey");
	}

	private void setCurrentValues()
	{
		SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();

		sabserver.setSummary(String.format(getString(R.string.pref_sabserver_summary),
			prefs.getString("sabserver", ""))); 
		sabapikey.setSummary(String.format(getString(R.string.pref_sabapikey_summary),
			prefs.getString("sabapikey", "")));
	}

	@Override protected void onResume()
	{
		super.onResume();
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
		setCurrentValues();
	}

	@Override protected void onPause()
	{
		super.onPause();
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		setCurrentValues();
	}
}
