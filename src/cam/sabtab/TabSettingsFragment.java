package cam.sabtab;
import cam.sabtab.model.SabControl;
import cam.sabtab.model.SabControlEvent;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;

public class TabSettingsFragment extends Fragment
{
	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state)
	{
		if(container == null) return null;

		return inflater.inflate(R.layout.tab_settings, container, false);
	}
}
