package com.example.dissertation.ui.notes;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.dissertation.DatabaseHelper;
import com.example.dissertation.R;

import java.util.ArrayList;

public class NotesFragment extends Fragment {

    private EditText editTextTitle;
    private EditText editTextContent;
    private ListView listViewNotes;
    private ArrayAdapter<String> adapter;
    private DatabaseHelper dbHelper;
    private ArrayList<Integer> noteIds;  // Ensure this is declared correctly

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notes, container, false);
        editTextTitle = view.findViewById(R.id.editTextTitle);
        editTextContent = view.findViewById(R.id.editTextContent);
        listViewNotes = view.findViewById(R.id.listViewNotes);
        Button buttonSave = view.findViewById(R.id.buttonSave);
        noteIds = new ArrayList<>();

        dbHelper = new DatabaseHelper(getActivity());
        loadNotes();

        buttonSave.setOnClickListener(v -> saveNote());

        listViewNotes.setOnItemClickListener((parent, view1, position, id) -> {
            String noteDetails = adapter.getItem(position);
            showNoteDetails(noteDetails);
        });

        listViewNotes.setOnItemLongClickListener((parent, view12, position, id) -> {
            confirmDeletion(noteIds.get(position));
            return true;
        });

        return view;
    }

    private void deleteNote(int noteId) {
        dbHelper.deleteNoteData(noteId);
        Toast.makeText(getActivity(), "Note deleted", Toast.LENGTH_SHORT).show();
        loadNotes();
    }

    private void confirmDeletion(int noteId) {
        new AlertDialog.Builder(requireActivity())
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to delete this note?")
                .setPositiveButton("Delete", (dialog, which) -> deleteNote(noteId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveNote() {
        String title = editTextTitle.getText().toString();
        String content = editTextContent.getText().toString();
        dbHelper.insertNoteData(title, content);
        Toast.makeText(getActivity(), "Saved", Toast.LENGTH_SHORT).show();
        loadNotes(); // Reload the list
    }

    @SuppressLint("Range")
    private void loadNotes() {
        Cursor cursor = dbHelper.readNoteData();
        ArrayList<String> notes = new ArrayList<>();
        noteIds.clear();
        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndex("noteID")); // Make sure "noteID" is spelled correctly
            String title = cursor.getString(cursor.getColumnIndex("title"));
            String content = cursor.getString(cursor.getColumnIndex("content"));
            notes.add(title + "\n" + content);
            noteIds.add(id);
        }
        adapter = new ArrayAdapter<>(requireActivity(), android.R.layout.simple_list_item_1, notes);
        listViewNotes.setAdapter(adapter);
    }

    private void showNoteDetails(String noteDetails) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle("Note Details");
        builder.setMessage(noteDetails);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
