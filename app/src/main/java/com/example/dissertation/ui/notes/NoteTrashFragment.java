package com.example.dissertation.ui.notes;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.dissertation.DatabaseHelper;
import com.example.dissertation.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class NoteTrashFragment extends Fragment {

    private ListView listViewTrash;
    private ArrayAdapter<String> adapter;
    private DatabaseHelper dbHelper;
    private ArrayList<Integer> noteIds;
    ArrayList<String> notes = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Set up the view and link our .xml file to it
        View view = inflater.inflate(R.layout.fragment_notetrash, container, false);

        // Initialize view by its specific ID
        listViewTrash = view.findViewById(R.id.listViewTrash);

        // Create an array list to hold the ID's
        noteIds = new ArrayList<>();

        // Initialize the DatabaseHelper to facilitate database operations (CRUD)
        dbHelper = new DatabaseHelper(getActivity());

        // Initialize the adapter, setting this fragment as context
        adapter = new ArrayAdapter<>(requireActivity(), android.R.layout.simple_list_item_1, notes);
        listViewTrash.setAdapter(adapter);

        loadTrashNotes(); // Load data early to ensure UI is populated before any user interaction

        // Click listener for restoring a note
        listViewTrash.setOnItemClickListener((parent, view1, position, id) -> {
            int noteId = noteIds.get(position);
            confirmRestoration(noteId);
        });

        // Long click listener for permanently deleting a note
        listViewTrash.setOnItemLongClickListener((parent, view1, position, id) -> {
            int noteId = noteIds.get(position);
            confirmPermanentDeletion(noteId);
            return true;
        });

        // Go back to notes when this button is pressed
        view.findViewById(R.id.buttonBackToNotes).setOnClickListener(v -> goToNotes());

        dbHelper.cleanUpTrash(); // Clean up old deleted notes

        return view;
    }

    // Handle loading trash notes
    @SuppressLint("Range")
    private void loadTrashNotes() {
        try (Cursor cursor = dbHelper.readDeletedNotes()){
            noteIds.clear();
            notes.clear();
            while (cursor.moveToNext()) {
                // Extract needed data from SQL
                int id = cursor.getInt(cursor.getColumnIndex("noteID"));
                String title = cursor.getString(cursor.getColumnIndex("title"));
                String content = cursor.getString(cursor.getColumnIndex("content"));
                long deletedDate = cursor.getLong(cursor.getColumnIndex("deletedDate"));

                // Format Date according to the standard format
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.UK);
                String deletedDateString = sdf.format(new Date(deletedDate * 1000)); // Convert to milliseconds

                // Add the extracted data
                notes.add(title + "\n" + content + "\n" + "Deleted: " + deletedDateString);
                noteIds.add(id);
            }
            adapter.notifyDataSetChanged();
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Error loading deleted notes: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Handle deletion and visual feedback for the user
    private void deleteFromTrash(int noteId) {
        try {
            dbHelper.deleteNotePermanently(noteId);
            Toast.makeText(getActivity(), "Note permanently deleted", Toast.LENGTH_SHORT).show();

            loadTrashNotes(); // Reload the trash notes to reflect the deletion
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Error deleting note permanently: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Show an alert dialog to make sure the user intends to delete
    private void confirmPermanentDeletion(int noteId) {
        new AlertDialog.Builder(requireActivity())
                .setTitle("Permanently Delete Note")
                .setMessage("Are you sure you want to permanently delete this note?")
                .setPositiveButton("Delete", (dialog, which) -> deleteFromTrash(noteId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Handle restoring and visual feedback for the user
    private void restoreNote(int noteId) {
        try {
            dbHelper.restoreNote(noteId);
            Toast.makeText(getActivity(), "Note Restored", Toast.LENGTH_SHORT).show();

            loadTrashNotes(); // Reload the trash notes to reflect the restoration
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Error restoring note: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Show an alert dialog to make sure the user intends to restore
    private void confirmRestoration(int noteId) {
        new AlertDialog.Builder(requireActivity())
                .setTitle("Restore Note")
                .setMessage("Are you sure you want to restore this note?")
                .setPositiveButton("Restore", (dialog, which) -> restoreNote(noteId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Navigate back to the notes section
    private void goToNotes() {
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
        navController.navigate(R.id.nav_notes);
    }
}
