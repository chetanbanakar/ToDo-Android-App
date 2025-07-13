package com.chetan.todo;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TaskAdapter adapter;
    private TaskDbHelper dbHelper;
    private List<Task> taskList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        dbHelper = new TaskDbHelper(this);
        taskList = new ArrayList<>();
        adapter = new TaskAdapter(taskList, dbHelper);
        recyclerView.setAdapter(adapter);

        // Swipe to delete
        androidx.recyclerview.widget.ItemTouchHelper itemTouchHelper = new androidx.recyclerview.widget.ItemTouchHelper(new androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(0, androidx.recyclerview.widget.ItemTouchHelper.LEFT | androidx.recyclerview.widget.ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(androidx.recyclerview.widget.RecyclerView recyclerView, androidx.recyclerview.widget.RecyclerView.ViewHolder viewHolder, androidx.recyclerview.widget.RecyclerView.ViewHolder target) {
                return false;
            }
            @Override
            public void onSwiped(androidx.recyclerview.widget.RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Task task = taskList.get(position);
                deleteTask(task, position);
            }
        });
        itemTouchHelper.attachToRecyclerView(recyclerView);

        loadTasks();

        ImageButton addButton = findViewById(R.id.button_add);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AddTaskActivity.class));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTasks();
        // Optionally set toolbar title if not already
        android.widget.TextView toolbarTitle = findViewById(R.id.text_toolbar_title);
        if (toolbarTitle != null) toolbarTitle.setText("To-Do List");
    }

    private void loadTasks() {
        taskList.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(TaskDbHelper.TABLE_NAME, null, null, null, null, null, null);
        while (cursor.moveToNext()) {
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(TaskDbHelper.COLUMN_ID));
            String title = cursor.getString(cursor.getColumnIndexOrThrow(TaskDbHelper.COLUMN_TITLE));
            boolean completed = cursor.getInt(cursor.getColumnIndexOrThrow(TaskDbHelper.COLUMN_COMPLETED)) == 1;
            long dueDate = 0;
            int dueDateIdx = cursor.getColumnIndex(TaskDbHelper.COLUMN_DUE_DATE);
            if (dueDateIdx != -1) {
                dueDate = cursor.getLong(dueDateIdx);
            }
            taskList.add(new Task(id, title, completed, dueDate));
        }
        cursor.close();
        adapter.notifyDataSetChanged();
    }

    private void deleteTask(Task task, int position) {
        // Delete from DB
        dbHelper.getWritableDatabase().delete(TaskDbHelper.TABLE_NAME, TaskDbHelper.COLUMN_ID + "=?", new String[]{String.valueOf(task.getId())});
        taskList.remove(position);
        adapter.notifyItemRemoved(position);
    }
}
