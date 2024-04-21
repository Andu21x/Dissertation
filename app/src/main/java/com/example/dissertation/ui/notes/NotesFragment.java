package com.example.dissertation.ui.notes;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.dissertation.DatabaseHelper;
import com.example.dissertation.R;

import java.util.ArrayList;

public class NotesFragment extends Fragment {

    private EditText editTextTitle, editTextContent;
    private ListView listViewNotes;
    private ArrayAdapter<String> adapter;
    private DatabaseHelper dbHelper;
    private ArrayList<Integer> noteIds;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notes, container, false);

        editTextTitle = view.findViewById(R.id.editTextTitle);
        editTextContent = view.findViewById(R.id.editTextContent);
        listViewNotes = view.findViewById(R.id.listViewNotes);
        Button buttonSave = view.findViewById(R.id.buttonSave);
        noteIds = new ArrayList<>();

        dbHelper = new DatabaseHelper(getActivity());

        loadNotes();  // Load data early to ensure UI is populated before any user interaction

        buttonSave.setOnClickListener(v -> saveNote());

        listViewNotes.setOnItemClickListener((parent, view1, position, id) -> {
            int noteId = noteIds.get(position);
            String noteDetails = adapter.getItem(position);
            assert noteDetails != null;
            String[] parts = noteDetails.split("\n", 2);
            String title = parts[0];
            String content = parts.length > 1 ? parts[1] : "";
            showUpdateDialog(noteId, title, content);
        });

        listViewNotes.setOnItemLongClickListener((parent, view12, position, id) -> {
            confirmDeletion(noteIds.get(position));
            return true;
        });

        return view;
    }

    private void saveNote() {
        String title = editTextTitle.getText().toString();
        String content = editTextContent.getText().toString();

        // Check if text fields are not empty when saveNote() is called, if they are, then insert and clear after
        if (!title.isEmpty() || !content.isEmpty()) {
            try {
                // Insert the values into the database table and show a toast pop-up alerting the user
                dbHelper.insertNote(title, content);
                Toast.makeText(getActivity(), "Saved", Toast.LENGTH_SHORT).show();

                // Clear all input fields after saving
                editTextTitle.setText("");
                editTextContent.setText("");

                loadNotes(); // Reload the notes

            } catch (Exception e) {
                Toast.makeText(getActivity(), "Error saving note: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getActivity(), "Please enter a title or content", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("Range")
    private void loadNotes() {
        Cursor cursor = dbHelper.readNote();
        ArrayList<String> notes = new ArrayList<>();
        noteIds.clear();
        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndex("noteID"));
            String title = cursor.getString(cursor.getColumnIndex("title"));
            String content = cursor.getString(cursor.getColumnIndex("content"));
            notes.add(title + "\n" + content);
            noteIds.add(id);
        }
        adapter = new ArrayAdapter<>(requireActivity(), android.R.layout.simple_list_item_1, notes);
        listViewNotes.setAdapter(adapter);
    }

    private void deleteNote(int noteId) {
        try {
            dbHelper.deleteNote(noteId);
            Toast.makeText(getActivity(), "Note deleted", Toast.LENGTH_SHORT).show();
            loadNotes(); // Reload the list of notes
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Error deleting note: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void confirmDeletion(int noteId) {
        new AlertDialog.Builder(requireActivity())
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to delete this note?")
                .setPositiveButton("Delete", (dialog, which) -> deleteNote(noteId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showUpdateDialog(final int noteId, String currentTitle, String currentContent) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());

        // Set up the input fields
        EditText editTextTitle = new EditText(getContext());
        editTextTitle.setInputType(InputType.TYPE_CLASS_TEXT);
        editTextTitle.setText(currentTitle);

        EditText editTextContent = new EditText(getContext());
        editTextContent.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editTextContent.setText(currentContent);

        // Layout to contain the EditText fields (AlertDialog doesn't work like it did for deletion methods)
        LinearLayout layout = new LinearLayout(getContext());

        layout.setOrientation(LinearLayout.VERTICAL); // They were left and right at first, make it one above the other
        layout.setPadding(40, 10, 40, 10); // Set padding around the layout

        layout.addView(editTextTitle);
        layout.addView(editTextContent);

        builder.setView(layout); // Set the view to AlertDialog

        // Set up the buttons
        builder.setPositiveButton("Update", (dialog, which) -> {
            try {
                String title = editTextTitle.getText().toString();
                String content = editTextContent.getText().toString();
                updateNote(noteId, title, content);
            } catch (Exception e) {
                Toast.makeText(getActivity(), "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void updateNote(int noteId, String title, String content) {
        try {
            dbHelper.updateNote(noteId, title, content);
            Toast.makeText(getActivity(), "Note Updated", Toast.LENGTH_SHORT).show();
            loadNotes(); // Reload the list of notes
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Failed to update note: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
