package com.chetan.todo;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class EditTaskActivity extends AppCompatActivity {
    private EditText editTextTitle;
    private Button buttonUpdate, buttonPickDueDate, buttonPickDueTime;
    private TaskDbHelper dbHelper;
    private long taskId;
    private long dueDateMillis = 0;
    private TextView textDueDate, textDueTime;
    private int pickedYear = -1, pickedMonth = -1, pickedDay = -1, pickedHour = -1, pickedMinute = -1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_task);

        editTextTitle = findViewById(R.id.edit_text_title);
        buttonUpdate = findViewById(R.id.button_update);
        buttonPickDueDate = findViewById(R.id.button_pick_due_date);
        buttonPickDueTime = findViewById(R.id.button_pick_due_time);
        textDueDate = findViewById(R.id.text_due_date);
        textDueTime = findViewById(R.id.text_due_time);
        dbHelper = new TaskDbHelper(this);

        Intent intent = getIntent();
        taskId = intent.getLongExtra("task_id", -1);
        String taskTitle = intent.getStringExtra("task_title");
        dueDateMillis = getDueDateFromDb(taskId);
        if (taskId == -1) {
            Toast.makeText(this, "Invalid task", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        editTextTitle.setText(taskTitle);
        updateDueDateText();

        buttonPickDueDate.setOnClickListener(v -> showDatePicker());
        buttonPickDueTime.setOnClickListener(v -> showTimePicker());

        buttonUpdate.setOnClickListener(v -> {
            String newTitle = editTextTitle.getText().toString().trim();
            if (newTitle.isEmpty()) {
                Toast.makeText(EditTaskActivity.this, "Please enter a task title", Toast.LENGTH_SHORT).show();
            } else {
                updateTaskInDb(newTitle, dueDateMillis);
                finish();
            }
        });
    }

    private void showDatePicker() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        if (dueDateMillis > 0) calendar.setTimeInMillis(dueDateMillis);
        new android.app.DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            pickedYear = year;
            pickedMonth = month;
            pickedDay = dayOfMonth;
            calendar.set(year, month, dayOfMonth, pickedHour != -1 ? pickedHour : 0, pickedMinute != -1 ? pickedMinute : 0, 0);
            dueDateMillis = calendar.getTimeInMillis();
            updateDueDateText();
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

    private void updateDueDateText() {
        if (dueDateMillis > 0) {
            java.text.DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(this);
            textDueDate.setText("Due: " + dateFormat.format(new java.util.Date(dueDateMillis)));
        } else {
            textDueDate.setText("No due date set");
        }
    }

    private void updateTaskInDb(String newTitle, long dueDate) {
        ContentValues values = new ContentValues();
        values.put(TaskDbHelper.COLUMN_TITLE, newTitle);
        values.put(TaskDbHelper.COLUMN_DUE_DATE, dueDate);
        dbHelper.getWritableDatabase().update(TaskDbHelper.TABLE_NAME, values, TaskDbHelper.COLUMN_ID + "=?", new String[]{String.valueOf(taskId)});
        if (dueDate > 0) {
            scheduleReminder(taskId, newTitle, dueDate);
        } else {
            cancelReminder(taskId);
        }
    }

    private void scheduleReminder(long taskId, String title, long dueDate) {
        android.app.AlarmManager alarmManager = (android.app.AlarmManager) getSystemService(android.content.Context.ALARM_SERVICE);
        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("task_title", title);
        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getBroadcast(this, (int) taskId, intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);
        alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP, dueDate, pendingIntent);
    }

    private void cancelReminder(long taskId) {
        android.app.AlarmManager alarmManager = (android.app.AlarmManager) getSystemService(android.content.Context.ALARM_SERVICE);
        Intent intent = new Intent(this, ReminderReceiver.class);
        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getBroadcast(this, (int) taskId, intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);
        alarmManager.cancel(pendingIntent);
    }

    private long getDueDateFromDb(long taskId) {
        long due = 0;
        android.database.Cursor cursor = dbHelper.getReadableDatabase().query(TaskDbHelper.TABLE_NAME, new String[]{TaskDbHelper.COLUMN_DUE_DATE}, TaskDbHelper.COLUMN_ID + "=?", new String[]{String.valueOf(taskId)}, null, null, null);
        if (cursor.moveToFirst()) {
            int idx = cursor.getColumnIndex(TaskDbHelper.COLUMN_DUE_DATE);
            if (idx != -1) due = cursor.getLong(idx);
        }
        cursor.close();
        return due;
    }
}
