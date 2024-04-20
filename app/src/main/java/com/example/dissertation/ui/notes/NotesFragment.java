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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notes, container, false);
        editTextTitle = view.findViewById(R.id.editTextTitle);
        editTextContent = view.findViewById(R.id.editTextContent);
        listViewNotes = view.findViewById(R.id.listViewNotes);
        Button buttonSave = view.findViewById(R.id.buttonSave);

        dbHelper = new DatabaseHelper(getActivity());
        loadNotes();

        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNote();
            }
        });

        return view;
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
        while (cursor.moveToNext()) {
            notes.add(cursor.getString(cursor.getColumnIndex("title")));
        }
        adapter = new ArrayAdapter<>(requireActivity(), android.R.layout.simple_list_item_1, notes);
        listViewNotes.setAdapter(adapter);
    }
}
