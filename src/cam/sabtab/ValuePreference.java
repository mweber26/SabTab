package cam.sabtab;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.preference.EditTextPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class ValuePreference extends EditTextPreference
{
	private TextView tv;

	public ValuePreference(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		setWidgetLayoutResource(R.layout.pref_value);
	}

	@Override protected void onBindView(View view)
	{
		super.onBindView(view);
		tv = (TextView)view.findViewById(R.id.pref_value_widget);
		setValue();
	}

	@Override protected void onDialogClosed(boolean positiveResult)
	{
		super.onDialogClosed(positiveResult);
		setValue();
	}

	private void setValue()
	{
		if(tv != null)
			tv.setText(getText());
	}
}
