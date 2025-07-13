package com.chetan.todo;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    private List<Task> taskList;
    private TaskDbHelper dbHelper;

    public TaskAdapter(List<Task> taskList, TaskDbHelper dbHelper) {
        this.taskList = taskList;
        this.dbHelper = dbHelper;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.titleText.setText(task.getTitle());
        holder.completedCheck.setChecked(task.isCompleted());
        holder.completedCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateTaskCompleted(task, isChecked);
        });
        holder.deleteButton.setOnClickListener(v -> {
            confirmDelete(holder.itemView, task, position);
        });
        holder.titleText.setOnClickListener(v -> {
            android.content.Context context = v.getContext();
            android.content.Intent intent = new android.content.Intent(context, EditTaskActivity.class);
            intent.putExtra("task_id", task.getId());
            intent.putExtra("task_title", task.getTitle());
            context.startActivity(intent);
        });
        // Show due date if set
        if (holder.dueDateText != null) {
            if (task.getDueDate() > 0) {
                java.text.DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(holder.itemView.getContext());
                holder.dueDateText.setText("Due: " + dateFormat.format(new java.util.Date(task.getDueDate())));
                holder.dueDateText.setVisibility(android.view.View.VISIBLE);
            } else {
                holder.dueDateText.setVisibility(android.view.View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    private void updateTaskCompleted(Task task, boolean completed) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("UPDATE " + TaskDbHelper.TABLE_NAME + " SET " + TaskDbHelper.COLUMN_COMPLETED + " = ? WHERE " + TaskDbHelper.COLUMN_ID + " = ?", new Object[]{completed ? 1 : 0, task.getId()});
        task.setCompleted(completed);
    }

    private void confirmDelete(View view, Task task, int position) {
        new AlertDialog.Builder(view.getContext())
                .setTitle("Delete Task")
                .setMessage("Are you sure you want to delete this task?")
                .setPositiveButton("Delete", (dialog, which) -> deleteTask(task, position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteTask(Task task, int position) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(TaskDbHelper.TABLE_NAME, TaskDbHelper.COLUMN_ID + " = ?", new String[]{String.valueOf(task.getId())});
        taskList.remove(position);
        notifyItemRemoved(position);
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView titleText;
        TextView dueDateText;
        CheckBox completedCheck;
        ImageButton deleteButton;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.text_title);
            dueDateText = itemView.findViewById(R.id.text_due_date);
            completedCheck = itemView.findViewById(R.id.checkbox_completed);
            deleteButton = itemView.findViewById(R.id.button_delete);
        }
    }
}
