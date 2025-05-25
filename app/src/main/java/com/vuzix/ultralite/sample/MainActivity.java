package com.vuzix.ultralite.sample;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Assuming R class is in this package or imported correctly.
// import com.vuzix.ultralite.sample.R; // If R is not automatically found

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity"; // For logging
    private Spinner matchesSpinner;
    private TextView scoreTextView;
    private ArrayList<MatchDetails> liveMatchesList = new ArrayList<>(); // Store fetched matches

    // Executor for background tasks
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // It's crucial that R.layout.activity_main refers to the XML file expected.
        // If the user has a different main layout file name, this would need to be adjusted by them.
        setContentView(R.layout.main_activity); 

        matchesSpinner = findViewById(R.id.matches_spinner);
        scoreTextView = findViewById(R.id.score_textview);

        // Set up the spinner with a default "Loading..." message initially
        ArrayAdapter<String> initialAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"Loading matches..."});
        initialAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        matchesSpinner.setAdapter(initialAdapter);
        matchesSpinner.setEnabled(false); // Disable spinner until data is loaded

        fetchMatchesAndUpdateSpinner();

        matchesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (liveMatchesList != null && position >= 0 && position < liveMatchesList.size()) {
                    MatchDetails selectedMatch = liveMatchesList.get(position);
                    if (selectedMatch != null && selectedMatch.getScore() != null) {
                         scoreTextView.setText(selectedMatch.getScore());
                         Log.d(TAG, "Displaying score for: " + selectedMatch.getMatchTitle() + " - " + selectedMatch.getScore());
                    } else if (selectedMatch != null) {
                        scoreTextView.setText("Score not available for this match.");
                         Log.d(TAG, "Score not available for: " + selectedMatch.getMatchTitle());
                    }
                } else {
                     // This case handles selection of placeholder items like "No matches available"
                     // or if liveMatchesList is unexpectedly empty when an item is somehow selected.
                     String selectedItemText = parent.getItemAtPosition(position).toString();
                     if (selectedItemText.equals("No matches available") ||
                         selectedItemText.equals("Failed to load matches") ||
                         selectedItemText.equals("Loading matches...")) {
                          scoreTextView.setText(""); // Clear score if a placeholder is selected
                     }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                scoreTextView.setText(""); // Clear score if nothing is selected
            }
        });
    }

    private void fetchMatchesAndUpdateSpinner() {
        executorService.execute(() -> {
            final ArrayList<MatchDetails> fetchedMatches = CricinfoLive.getLiveMatchesFromRSS();

            mainThreadHandler.post(() -> {
                liveMatchesList.clear();
                if (fetchedMatches != null && !fetchedMatches.isEmpty()) {
                    liveMatchesList.addAll(fetchedMatches);
                    List<String> matchTitles = new ArrayList<>();
                    for (MatchDetails match : liveMatchesList) {
                        matchTitles.add(match.getMatchTitle());
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this,
                            android.R.layout.simple_spinner_item, matchTitles);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    matchesSpinner.setAdapter(adapter);
                    matchesSpinner.setEnabled(true);
                    
                    if (!liveMatchesList.isEmpty()) {
                        matchesSpinner.setSelection(0); 
                        // Score for the first item will be set by onItemSelected listener
                    } else {
                         scoreTextView.setText(""); 
                    }
                } else {
                    List<String> statusMessages = new ArrayList<>();
                    if (fetchedMatches == null) { 
                        statusMessages.add("Failed to load matches");
                        Toast.makeText(MainActivity.this, "Error fetching matches. Check network.", Toast.LENGTH_LONG).show();
                    } else { 
                        statusMessages.add("No matches available");
                        Toast.makeText(MainActivity.this, "No live matches found.", Toast.LENGTH_LONG).show();
                    }
                    ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(MainActivity.this,
                            android.R.layout.simple_spinner_item, statusMessages);
                    statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    matchesSpinner.setAdapter(statusAdapter);
                    matchesSpinner.setEnabled(false); 
                    scoreTextView.setText(""); 
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
