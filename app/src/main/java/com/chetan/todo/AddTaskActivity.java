package com.chetan.todo;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class AddTaskActivity extends AppCompatActivity {
    private EditText editTextTitle;
    private Button buttonSave, buttonPickDueDate, buttonPickDueTime;
    private TaskDbHelper dbHelper;
    private TextView textDueDate, textDueTime;
    private long dueDateMillis = 0;
    private int pickedYear = -1, pickedMonth = -1, pickedDay = -1, pickedHour = -1, pickedMinute = -1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        editTextTitle = findViewById(R.id.edit_text_title);
        buttonSave = findViewById(R.id.button_save);
        buttonPickDueDate = findViewById(R.id.button_pick_due_date);
        buttonPickDueTime = findViewById(R.id.button_pick_due_time);
        textDueDate = findViewById(R.id.text_due_date);
        textDueTime = findViewById(R.id.text_due_time);
        dbHelper = new TaskDbHelper(this);

        NotificationUtils.createNotificationChannel(this);

        buttonPickDueDate.setOnClickListener(v -> showDatePicker());
        buttonPickDueTime.setOnClickListener(v -> showTimePicker());

        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = editTextTitle.getText().toString().trim();
                if (title.isEmpty()) {
                    Toast.makeText(AddTaskActivity.this, "Please enter a task title", Toast.LENGTH_SHORT).show();
                } else {
                    addTaskToDb(title, dueDateMillis);
                    NotificationUtils.sendTaskAddedNotification(AddTaskActivity.this, title);
                    finish();
                }
            }
        });
    }

    private void showDatePicker() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        new android.app.DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            pickedYear = year;
            pickedMonth = month;
            pickedDay = dayOfMonth;
            calendar.set(year, month, dayOfMonth, pickedHour != -1 ? pickedHour : 0, pickedMinute != -1 ? pickedMinute : 0, 0);
            dueDateMillis = calendar.getTimeInMillis();
            java.text.DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(this);
            textDueDate.setText("Due: " + dateFormat.format(new java.util.Date(dueDateMillis)));
            updateDueTimeText();
        }, calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int hour = pickedHour != -1 ? pickedHour : calendar.get(java.util.Calendar.HOUR_OF_DAY);
        int minute = pickedMinute != -1 ? pickedMinute : calendar.get(java.util.Calendar.MINUTE);
        new android.app.TimePickerDialog(this, (view, hourOfDay, minute1) -> {
            pickedHour = hourOfDay;
            pickedMinute = minute1;
            if (pickedYear != -1 && pickedMonth != -1 && pickedDay != -1) {
                calendar.set(pickedYear, pickedMonth, pickedDay, pickedHour, pickedMinute, 0);
                dueDateMillis = calendar.getTimeInMillis();
            }
            updateDueTimeText();
        }, hour, minute, true).show();
    }

    private void updateDueTimeText() {
        if (pickedHour != -1 && pickedMinute != -1) {
            textDueTime.setText(String.format("Due Time: %02d:%02d", pickedHour, pickedMinute));
        } else {
            textDueTime.setText("No due time set");
        }
    }

    private void addTaskToDb(String title, long dueDate) {
        ContentValues values = new ContentValues();
        values.put(TaskDbHelper.COLUMN_TITLE, title);
        values.put(TaskDbHelper.COLUMN_COMPLETED, 0);
        values.put(TaskDbHelper.COLUMN_DUE_DATE, dueDate);
        long taskId = dbHelper.getWritableDatabase().insert(TaskDbHelper.TABLE_NAME, null, values);
        if (dueDate > 0) {
            scheduleReminder(taskId, title, dueDate);
        }
    }

    private void scheduleReminder(long taskId, String title, long dueDate) {
        android.app.AlarmManager alarmManager = (android.app.AlarmManager) getSystemService(android.content.Context.ALARM_SERVICE);
        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("task_title", title);
        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getBroadcast(this, (int) taskId, intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);
        alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP, dueDate, pendingIntent);
    }
}
