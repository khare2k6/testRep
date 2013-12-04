package ak.expensemanager.ui;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import ak.expensemanager.R;
import ak.expensemanager.category.CategoryInfoSharedPref;
import ak.expensemanager.category.ICategory;
import ak.expensemanager.db.IRetrieveExpenses;
import ak.expensemanager.db.RetrieveExpenses;
import ak.expensemanager.debug.IDebugTag;
import ak.expensemanager.model.Category;
import ak.expensemanager.model.Expense;
import ak.expensemanager.model.SmsDecipher;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

public class FloatingActivity extends Activity 
	implements DatePickerDialog.OnDateSetListener, 
	TimePickerDialog.OnTimeSetListener, 
	View.OnClickListener {

	final static String TAG = IDebugTag.ankitTag
			+ FloatingActivity.class.getSimpleName();
	EditText et_amount = null;
	Spinner spinner = null;
	ArrayAdapter<String> adapter = null;
	String[] arrCategory = null;
	int amt;
	EditText et_notes = null;
	Button btn_submit = null;
	TextView txtDate;
	Button btnChangeDate;
	TextView txtTime;
	Button btnChangeTime;
	
	IRetrieveExpenses expenses = null;
	ICategory category = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");
		
		setContentView(R.layout.activity_floating);
		
		setupViews(getIntent());
		
		setCurrentDate();
		
		expenses = new RetrieveExpenses(this);
		category = new CategoryInfoSharedPref(this);

		Window window = getWindow();
		// window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
		// WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
		// window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
				WindowManager.LayoutParams.WRAP_CONTENT);
		window.setGravity(Gravity.CENTER_HORIZONTAL);
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.btn_change_date:
			changeDate();
			break;
		case R.id.btn_change_time:
			changeTime();
			break;
		case R.id.btn_submitExp:
			submit();
			break;
		}
	}
	
	private void setupViews(Intent intent) {
		txtDate = (TextView) findViewById(R.id.date);
		txtTime = (TextView) findViewById(R.id.time);
		
		btnChangeDate = (Button) findViewById(R.id.btn_change_date);
		btnChangeDate.setOnClickListener(this);
		btnChangeTime = (Button) findViewById(R.id.btn_change_time);
		btnChangeTime.setOnClickListener(this);
		
		spinner = (Spinner) findViewById(R.id.spn_category);
		et_amount = (EditText) findViewById(R.id.et_amount);
		et_notes = (EditText) findViewById(R.id.et_notes);
		btn_submit = (Button) findViewById(R.id.btn_submitExp);
		btn_submit.setOnClickListener(this);
		
		if (intent != null) {
			decipherAmount(intent.getStringExtra(IDebugTag.MESSAGE));
		}
		
	}
	
	private void decipherAmount(String sms) {
		// Ankit : This logic should be moved out of this activity
		int amount = new SmsDecipher(sms,this).getAmount();
		if (amount > 0) {
			et_amount.setText("" + amount);
		}
	}
	
	private void setCurrentDate() {
		Calendar cal = Calendar.getInstance();
		
		setDate(cal.get(Calendar.YEAR), 
				cal.get(Calendar.MONTH),
				cal.get(Calendar.DAY_OF_MONTH));
		
		setTime(cal.get(Calendar.HOUR), 
				cal.get(Calendar.MINUTE));
		
		txtDate.setTag(cal);
	}
	
	private void setDate(int year, int month, int day) {
		txtDate.setText(day + "/" + month + "/" + year);
	}
	
	private void setTime(int hour, int mins) {
		txtTime.setText(hour + ":" +  mins);
	}

	private void changeDate() {
		DatePickerFragment dateFragment = new DatePickerFragment();
		dateFragment.setListener(this);
		dateFragment.show(getFragmentManager(), "datePicker");
	}
	
	private void changeTime() {
		TimePickerFragment timeFragment = new TimePickerFragment();
		timeFragment.setListener(this);
		timeFragment.show(getFragmentManager(), "TimePicker");
	}
	
	private void submit() {
		String strAmount = et_amount.getText().toString();
		String strCategory = spinner.getSelectedItem().toString();
		String note = et_notes.getText().toString();
		
		Log.d(TAG, "click: " + category + " and " + note);
		if (strAmount.isEmpty()) {
			Toast.makeText(FloatingActivity.this,
					"Please Enter Amount spent", Toast.LENGTH_SHORT).show();
			return;
		}
		
		Calendar cal = (Calendar) txtDate.getTag();
		Category category = new Category(strCategory, -1);
		int amount = Integer.parseInt(strAmount);
		
		expenses.addExpense(new Expense(amount, category, note, cal));
		
		finish();
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Log.d(TAG, "onNewintent");
		decipherAmount(intent.getStringExtra(IDebugTag.MESSAGE));
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();

	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		adapter = null;
		spinner.setAdapter(null);
	}

	@Override
	protected void onResume() {
		super.onResume();

		

		ArrayList<String> list = getCategoryList();
		adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, list);
		spinner.setAdapter(adapter);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		if (spinner.getCount() == 0) {
			Toast.makeText(this, "Adding Default Category!", Toast.LENGTH_LONG)
					.show();
			category.addCategory(IDebugTag.DEFAULT_CATEGORY);
			finish();
		}
	}

	private ArrayList<String> getCategoryList() {
		ArrayList<String> list = new ArrayList<String>();
		list = new ArrayList<String>(category.getAllCategory().keySet());
		/*
		 * list.add("Fuel"); list.add("Eating Out"); list.add("Misc");
		 */

		return list;
	}

	int getAmountFromEditText() {
		String amt = et_amount.getText().toString();
		if (amt.isEmpty()) {
			Toast.makeText(this, "No Amount entered", Toast.LENGTH_SHORT)
					.show();
			return 0;
		} else {
			return Integer.parseInt(amt);
		}
	}

	
	public static class DatePickerFragment extends DialogFragment {
		DatePickerDialog.OnDateSetListener mListener;
		
		public void setListener(DatePickerDialog.OnDateSetListener listener) {
			mListener = listener;
		}
		
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			// Use the current date as the default date in the picker
			final Calendar c = Calendar.getInstance();
			int year = c.get(Calendar.YEAR);
			int month = c.get(Calendar.MONTH);
			int day = c.get(Calendar.DAY_OF_MONTH);

			// Create a new instance of DatePickerDialog and return it
			return new DatePickerDialog(getActivity(), mListener, year, month, day);
		}
	}
	
	@Override
	public void onDateSet(DatePicker view, int year, int month, int day) {
		Calendar cal = (Calendar) txtDate.getTag();
		if (cal == null) {
			cal = Calendar.getInstance();
			txtDate.setTag(cal);
		}
		cal.set(year, month, day);
		
		setDate(year, month, day);
	}

	public static class TimePickerFragment extends DialogFragment {
		TimePickerDialog.OnTimeSetListener mListener;
		
		public void setListener(TimePickerDialog.OnTimeSetListener listener) {
			mListener = listener;
		}
		
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			// Use the current date as the default date in the picker
			final Calendar c = Calendar.getInstance();
			int hour = c.get(Calendar.HOUR_OF_DAY);
			int mins = c.get(Calendar.MINUTE);

			// Create a new instance of DatePickerDialog and return it
			return new TimePickerDialog(getActivity(), mListener, hour, mins, false);
		}
	}

	@Override
	public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
		Calendar cal = (Calendar) txtDate.getTag();
		if (cal == null) {
			cal = Calendar.getInstance();
			txtDate.setTag(cal);
		}
		
		cal.set(cal.get(Calendar.YEAR), 
				cal.get(Calendar.MONTH),
				cal.get(Calendar.DAY_OF_MONTH),
				hourOfDay,
				minute);
		
		setTime(hourOfDay, minute);
	}
}
