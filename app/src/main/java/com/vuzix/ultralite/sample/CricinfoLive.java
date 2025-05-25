package com.vuzix.ultralite.sample;

import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class CricinfoLive {
    private static final String TAG = "CricinfoFetcher";
    private static final String CRICINFO_BASE_URL = "https://www.espncricinfo.com";
    private static final String CRICINFO_LIVE_SCORES_URL = CRICINFO_BASE_URL + "/live-cricket-score";

    // Testable method with HTML content as input
    public static ArrayList<MatchDetails> getLiveMatches(String htmlContent) {
        Log.d(TAG, "getLiveMatches(htmlContent) called");
        ArrayList<MatchDetails> matches = new ArrayList<>();
        // The base URL is needed by Jsoup.parse to resolve relative URLs
        Document doc = Jsoup.parse(htmlContent, CRICINFO_BASE_URL);

        // Attempt to find a container for all match cards. This is a guess.
        // Common patterns involve divs with classes like "match-feed", "live-matches", "score-card-list"
        // Then, individual match elements often have classes like "match-card", "fixture", "list-item"
        // Let's try a general approach first, looking for elements that seem to contain match details.
        // Based on typical sports websites, match info is often in a structured list or series of cards.
        // We'll look for elements that have a distinct title and a link associated with them.
        // A common structure might be:
        // <div class="match-container-class">
        //   <a href="/match-url-1">
        //     <h3 class="match-title-class">Match Title 1</h3>
        //     ... other details ...
        //   </a>
        // </div>
        // Or
        // <div class="match-fixture-class">
        //  <div class="match-info">
        //      <a href="/match-url-2" class="match-title-link">
        //          <span class="match-title">Match Title 2</span>
        //      </a>
        //  </div>
        //  ...
        // </div>

        // Trying a selector that might identify individual match components.
        // This selector looks for divs that might be individual match containers.
        // It's a broad guess and would need refinement if the HTML structure is known.
        Elements matchElements = doc.select("div.ds-p-4"); // A common padding class, might indicate a card.
        // Or try: "div.ci-match-card" or "li.match-item" if such specific classes exist.

        if (matchElements.isEmpty()) {
            // Fallback or alternative selector if the first one doesn't yield results
            // This selector targets list items within a specific type of layout often used for scores.
            matchElements = doc.select("div.ds-flex.ds-flex-col.ds-mt-2 > div.ds-mb-4");
            Log.d(TAG, "Initial selector 'div.ds-p-4' found 0 elements. Trying 'div.ds-flex.ds-flex-col.ds-mt-2 > div.ds-mb-4'");
        }

        if (matchElements.isEmpty()) {
            // Broader search for links that could be matches
            matchElements = doc.select("a[href*='/live-cricket-scores/']"); // Note: Changed from /live-cricket-score/ to /live-cricket-scores/ to match example
            Log.d(TAG, "Second selector also found 0. Trying 'a[href*=/live-cricket-scores/]'");
        }


        Log.d(TAG, "Found " + matchElements.size() + " potential match elements.");

        for (Element matchElement : matchElements) {
            String title = "";
            String matchUrl = "";

            // Try to extract title - often in a heading or a specific span
            // Look for prominent text elements within the matchElement
            Element titleElement = matchElement.selectFirst("p.ds-text-tight-m.ds-font-bold.ds-truncate.ds-text-typo"); // Common for titles
            if (titleElement == null) {
                titleElement = matchElement.selectFirst("span[class*='title'], h2, h3, p.ci-match-title"); // More generic title selectors
            }
            if (titleElement == null && matchElement.tagName().equals("a")) { // If the matchElement itself is an 'a' tag
                // Attempt to get text from a child span or directly from the 'a' tag
                Element spanInLink = matchElement.selectFirst("span");
                if (spanInLink != null) {
                    title = spanInLink.text();
                } else {
                    title = matchElement.text();
                }
            }


            if (titleElement != null) {
                title = titleElement.text();
            }

            // Try to extract URL - usually in an 'a' tag's href
            Element linkElement = matchElement.selectFirst("a[href]");
            if (linkElement == null && matchElement.tagName().equals("a")) { // If the matchElement itself is an 'a' tag
                linkElement = matchElement;
            }


            if (linkElement != null) {
                matchUrl = linkElement.absUrl("href"); // absUrl ensures it's absolute based on CRICINFO_BASE_URL
            }

            // Basic validation: Ensure URL is for a specific match and not a generic link
            // Changed /live-cricket-score/ to /live-cricket-scores/ to match example and likely real URL structure
            if (!title.isEmpty() && !matchUrl.isEmpty() && matchUrl.contains("/live-cricket-scores/")) {
                // Further filter out non-match links if possible, e.g. by checking for keywords in title or URL structure
                if (title.matches(".* vs .*") || title.toLowerCase().contains("match")) { // Simple heuristic for a match title
                    matches.add(new MatchDetails(title, matchUrl));
                    Log.d(TAG, "Found match: " + title + " - " + matchUrl);
                } else {
                    Log.d(TAG, "Filtered out (likely not a match title): " + title + " - " + matchUrl);
                }
            } else {
                Log.d(TAG, "Skipping element, title or URL missing or invalid. Title: '" + title + "', URL: '" + matchUrl + "'");
            }
        }
        // Removed catch (IOException e) because Jsoup.parse doesn't throw it for string input
        // Consider adding a general catch (Exception e) if complex parsing might fail
        Log.d(TAG, "getLiveMatches(htmlContent) finished, found " + matches.size() + " matches.");
        return matches;
    }

    // Public method that fetches live data
    public static ArrayList<MatchDetails> getLiveMatches() {
        Log.d(TAG, "getLiveMatches (network) called");
        try {
            Document doc = Jsoup.connect(CRICINFO_LIVE_SCORES_URL)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .get();
            Log.d(TAG, "Successfully fetched HTML from: " + CRICINFO_LIVE_SCORES_URL);
            return getLiveMatches(doc.html()); // Call the testable method
        } catch (IOException e) {
            Log.e(TAG, "Error fetching live matches from network: " + e.getMessage());
            return new ArrayList<>(); // Return empty list on network error
        }
    }

    protected static int selectedMatchIndex=0;

    protected static String selectedMatchTitle="";

    // Testable method with HTML content as input
    public static String getLiveScoreOfSelectedMatch(String matchUrl, String htmlContent) {
        Log.d(TAG, "getLiveScoreOfSelectedMatch(htmlContent) called for URL: " + matchUrl);
        String score = "Score not available";
        Document doc = Jsoup.parse(htmlContent, matchUrl); // Use matchUrl as base URI for parsing this specific page

        // Strategy to find the score:
        // 1. Look for highly specific selectors that often contain live scores.
        // 2. If not found, try slightly more generic selectors related to scores or team info.
        // 3. As a fallback, look for text patterns matching cricket scores.

        Element scoreElement;

        // Attempt 1: More specific selectors (these are educated guesses)
        // Common classes for live scores might include "current-score", "live-score", "match-score"
        // ESPNCricinfo uses complex, often auto-generated class names.
        // Let's try to find elements that contain score-like text.
        // Looking for something like: <div class="ds-text-compact-m ...">TEAM 123/4</div>
        // Or: <div class="ci-score-overview ..."> <span>TEAM</span> <span>123/4</span> ... </div>

        // Selector based on common ESPNCricinfo structure for displaying scores prominently
        scoreElement = doc.selectFirst("div.ds-text-compact-m.ds-text-typo-title.ds-text-right.ds-whitespace-nowrap");
        if (scoreElement != null) {
            score = scoreElement.text().trim();
            Log.d(TAG, "Found score with selector 1: " + score);
            if (score.matches(".+\\s+\\d+/\\d+.*")) { // Basic validation: "TEAM 123/4"
                return score;
            }
        }

        // Attempt 2: Try to find elements that show team scores separately and combine them
        Elements teamNameElements = doc.select("p.ds-text-tight-m.ds-font-bold.ds-truncate.ds-text-typo"); // Team names
        Elements teamScoreElements = doc.select("div.ds-text-compact-m.ds-text-typo-title.ds-text-right.ds-whitespace-nowrap"); // Scores like 123/4

        if (!teamNameElements.isEmpty() && !teamScoreElements.isEmpty()) {
            // This assumes the first team name corresponds to the first score, etc.
            // This might need more sophisticated pairing logic if the structure is complex.
            for (int i = 0; i < teamNameElements.size() && i < teamScoreElements.size(); i++) {
                String teamName = teamNameElements.get(i).text().trim();
                String teamScoreText = teamScoreElements.get(i).text().trim();
                if (!teamName.isEmpty() && !teamScoreText.isEmpty() && teamScoreText.matches("\\d+/\\d+.*")) {
                    score = teamName + " " + teamScoreText;
                    Log.d(TAG, "Found score with selector combination 2: " + score);
                    // Potentially look for "overs" information nearby if needed
                    Element oversElement = teamScoreElements.get(i).nextElementSibling(); // Or parent().selectFirst(...)
                    if (oversElement != null && oversElement.text().contains("overs")) {
                        score += " (" + oversElement.text().trim() + ")";
                    }
                    return score; // Return the first combined score found
                }
            }
        }

        // Attempt 3: More generic selectors, looking for score patterns
        // This looks for spans that might contain the score, often styled differently.
        Elements potentialScoreElements = doc.select("div.ds-flex.ds-items-center.ds-justify-between.ds-mb-1 > div > span.ds-text-compact-s");
        if (!potentialScoreElements.isEmpty()) {
            for (Element el : potentialScoreElements) {
                String potentialScoreText = el.text().trim();
                Log.d(TAG, "Checking potential score (selector 3): " + potentialScoreText);
                if (potentialScoreText.matches(".+\\s+\\d+/\\d+.*") || potentialScoreText.matches("\\d+/\\d+.*")) {
                    score = potentialScoreText;
                    Log.d(TAG, "Found score with selector 3: " + score);
                    return score;
                }
            }
        }

        // Attempt 4: Fallback - Select all prominent text elements and check for score patterns.
        // This could be divs or spans with "score" in their class name, or just generally large/bold text.
        Elements genericElements = doc.select("div[class*='score'], span[class*='score'], p[class*='score'], div.ds-text-title-s, div.ds-text-typo-title");
        for (Element el : genericElements) {
            String text = el.text().trim();
            Log.d(TAG, "Checking generic element text: " + text);
            // Regex for "TEAM_NAME (optional) SCORE/WICKETS (OVERS optional)"
            if (text.matches(".*\\d+/\\d+\\s*(\\(.*\\))?.*") || text.matches(".*\\d+/\\d+.*")) {
                score = text;
                Log.d(TAG, "Found score by generic element text pattern: " + score);
                // Attempt to refine if it's too broad
                if (score.length() > 50) { // If the string is too long, it might be a summary, not the main score
                    Log.d(TAG, "Score string too long, attempting to find a more concise part.");
                    // Try to extract a more specific part using regex again, if possible
                }
                return score;
            }
        }

        // If no specific score element found, try to find a general match status
        if (score.equals("Score not available")) {
            Element statusElement = doc.selectFirst("p.ds-text-tight-s.ds-font-regular.ds-line-clamp-2.ds-text-typo");
            if (statusElement != null) {
                score = statusElement.text().trim();
                Log.d(TAG, "Found match status as fallback: " + score);
            } else {
                Log.d(TAG, "No suitable score or status elements found after all attempts.");
            }
        }
        Log.d(TAG, "Returning score for " + matchUrl + " from HTML content: " + score);
        return score;
}

// Public method that fetches live data
public static String getLiveScoreOfSelectedMatch(String matchUrl) {
    Log.d(TAG, "getLiveScoreOfSelectedMatch (network) called for URL: " + matchUrl);
    String score = "Score not available";

    if (matchUrl == null || matchUrl.isEmpty()) {
        Log.e(TAG, "Match URL is null or empty.");
        return score;
    }

    try {
        Document doc = Jsoup.connect(matchUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .timeout(15000) // 15 seconds timeout
                .get();
        Log.d(TAG, "Successfully fetched HTML from: " + matchUrl);
        return getLiveScoreOfSelectedMatch(matchUrl, doc.html()); // Call the testable method
    } catch (IOException e) {
        Log.e(TAG, "Error fetching score for " + matchUrl + " from network: " + e.getMessage());
    } catch (Exception e) { // Catching other potential parsing errors from network fetch
        Log.e(TAG, "Error parsing score for " + matchUrl + " from network: " + e.getMessage());
    }
    return score; // Return default score on error
}

public static List<String> fetchLiveScores() {
    List<MatchDetails> matchDetails=CricinfoLive.getLiveMatches();
    final List<String> fetchedData= new ArrayList<>();
    for(MatchDetails matchDetails1:matchDetails) {
        fetchedData.add(matchDetails1.getMatchTitle());
    }
    return fetchedData;
}


}