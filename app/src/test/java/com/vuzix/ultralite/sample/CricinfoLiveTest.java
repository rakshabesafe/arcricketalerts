package com.vuzix.ultralite.sample;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.ArrayList;

// Imports needed for testing with mock HTML
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class CricinfoLiveTest {

    @Test
    public void testGetLiveMatches_Success() {
        // Mock HTML based on the selectors used in CricinfoLive.getLiveMatches(String htmlContent)
        // Main selector: "div.ds-p-4"
        // Title selector: "p.ds-text-tight-m.ds-font-bold.ds-truncate.ds-text-typo"
        // Link selector: "a[href]"
        // URL must contain "/live-cricket-scores/" and title "vs" or "match"
        String mockHtml_liveMatches = "<html><body>" +
            "<div class='ds-p-4'>" +
            "  <a href='/live-cricket-scores/series-1/match-1-aus-vs-eng'>" +
            "    <p class='ds-text-tight-m ds-font-bold ds-truncate ds-text-typo'>Australia vs England, 1st Test</p>" +
            "  </a>" +
            "</div>" +
            "<div class='ds-p-4'>" + // This one should be skipped due to no "vs" or "match" in title and bad link
            "  <a href='/live-cricket-news/some-other-page'>" +
            "    <p class='ds-text-tight-m ds-font-bold ds-truncate ds-text-typo'>Some News Article</p>" +
            "  </a>" +
            "</div>" +
            "<div class='ds-p-4'>" +
            "  <a href='/live-cricket-scores/series-2/match-2-ind-vs-sa'>" +
            "    <p class='ds-text-tight-m ds-font-bold ds-truncate ds-text-typo'>India vs South Africa, 2nd ODI Match</p>" +
            "  </a>" +
            "</div>" +
            "</body></html>";

        ArrayList<MatchDetails> matches = CricinfoLive.getLiveMatches(mockHtml_liveMatches);

        assertNotNull("Matches list should not be null", matches);
        assertEquals("Should find 2 matches", 2, matches.size());

        // Check details of the first match
        MatchDetails match1 = matches.get(0);
        assertEquals("Match 1 title is incorrect", "Australia vs England, 1st Test", match1.getMatchTitle());
        assertEquals("Match 1 URL is incorrect", "https://www.espncricinfo.com/live-cricket-scores/series-1/match-1-aus-vs-eng", match1.getMatchUrl());

        // Check details of the second match
        MatchDetails match2 = matches.get(1);
        assertEquals("Match 2 title is incorrect", "India vs South Africa, 2nd ODI Match", match2.getMatchTitle());
        assertEquals("Match 2 URL is incorrect", "https://www.espncricinfo.com/live-cricket-scores/series-2/match-2-ind-vs-sa", match2.getMatchUrl());
    }

    @Test
    public void testGetLiveMatches_NoMatchesFound() {
        String mockHtml_noMatches = "<html><body>" +
            "<div>Some random content</div>" +
            "<p>No matches here, just some text.</p>" +
            "<div class='ds-p-4'>" + // Structure might be similar, but content doesn't match criteria
            "  <a href='/news/some-old-news'>" +
            "    <p class='ds-text-tight-m ds-font-bold ds-truncate ds-text-typo'>Old News Story</p>" +
            "  </a>" +
            "</div>" +
            "</body></html>";

        ArrayList<MatchDetails> matches = CricinfoLive.getLiveMatches(mockHtml_noMatches);

        assertNotNull("Matches list should not be null", matches);
        assertTrue("Matches list should be empty", matches.isEmpty());
    }

    @Test
    public void testGetLiveMatches_MalformedHtml() {
        // Test with HTML that might cause parsing issues or missing elements
        // Jsoup is generally robust, so this tests how our logic handles missing pieces.
        String mockHtml_malformed = "<html><body>" +
            "<div class='ds-p-4'>" + // Missing link
            "    <p class='ds-text-tight-m ds-font-bold ds-truncate ds-text-typo'>Incomplete Match vs Test</p>" +
            "</div>" +
            "<div class='ds-p-4'>" +
            "  <a href='/live-cricket-scores/series-good/match-good-valid-vs-team'></a>" + // Missing title paragraph
            "</div>" +
             "<a href='/live-cricket-scores/series-x/match-y-teamA-vs-teamB'>" + // Using fallback selector for a direct link
            "    TeamA vs TeamB Direct Link Match" +
            "  </a>" +
            "</body></html>";
        
        ArrayList<MatchDetails> matches = CricinfoLive.getLiveMatches(mockHtml_malformed);

        assertNotNull("Matches list should not be null", matches);
        // Expecting one match from the direct link fallback, as it has "vs" and a valid URL.
        // The other two are expected to fail due to missing title/link or title not matching "vs/match" criteria
        // after title extraction.
        assertEquals("Should find 1 match from fallback", 1, matches.size());
        if (matches.size() == 1) {
            assertEquals("Fallback match title incorrect", "TeamA vs TeamB Direct Link Match", matches.get(0).getMatchTitle());
            assertEquals("Fallback match URL incorrect", "https://www.espncricinfo.com/live-cricket-scores/series-x/match-y-teamA-vs-teamB", matches.get(0).getMatchUrl());
        }
    }

    // --- Tests for getLiveScoreOfSelectedMatch ---

    @Test
    public void testGetLiveScoreOfSelectedMatch_Success_PrimarySelector() {
        // Mock HTML for a specific match page.
        // Selector 1: "div.ds-text-compact-m.ds-text-typo-title.ds-text-right.ds-whitespace-nowrap"
        String mockMatchPageHtml = "<html><body>" +
            "<div class='something-else'>Other data</div>" +
            "<div class='ds-text-compact-m ds-text-typo-title ds-text-right ds-whitespace-nowrap'>" +
            "  Team X 123/4" + // Expected score format
            "</div>" +
            "<div class='ds-text-compact-m ds-text-typo-title ds-text-right ds-whitespace-nowrap'>" + // Decoy
            "  Commentary line" +
            "</div>" +
            "</body></html>";
        
        String matchUrl = "https://www.espncricinfo.com/live-cricket-scores/series-abc/match-xyz";
        String score = CricinfoLive.getLiveScoreOfSelectedMatch(matchUrl, mockMatchPageHtml);

        assertEquals("Score was not extracted correctly by primary selector", "Team X 123/4", score);
    }

    @Test
    public void testGetLiveScoreOfSelectedMatch_Success_CombinedSelector() {
        // Selector 2: Team name "p.ds-text-tight-m.ds-font-bold.ds-truncate.ds-text-typo"
        //             Score part "div.ds-text-compact-m.ds-text-typo-title.ds-text-right.ds-whitespace-nowrap"
        //             Optional overs "nextElementSibling" of score part, containing "overs"
        String mockMatchPageHtml = "<html><body>" +
            "<div>" +
            "  <p class='ds-text-tight-m ds-font-bold ds-truncate ds-text-typo'>Sri Lanka</p>" +
            "  <div class='ds-text-compact-m ds-text-typo-title ds-text-right ds-whitespace-nowrap'>178/7</div>" +
            "  <span class='ds-text-compact-xs ds-mr-0.5'> (19.5/20 ov)</span>" + // Example of overs, though current logic might not pick this exact structure up
            "</div>" +
            "<div>" + // Another team, should pick the first valid one
            "  <p class='ds-text-tight-m ds-font-bold ds-truncate ds-text-typo'>Australia</p>" +
            "  <div class='ds-text-compact-m ds-text-typo-title ds-text-right ds-whitespace-nowrap'>Yet to bat</div>" +
            "</div>" +
            "</body></html>";

        String matchUrl = "https://www.espncricinfo.com/live-cricket-scores/series-sl/match-sl-aus";
        String score = CricinfoLive.getLiveScoreOfSelectedMatch(matchUrl, mockMatchPageHtml);
        
        // Current logic for combined selector might grab "Sri Lanka 178/7" and then look for overs.
        // The overs part is heuristic, so let's check the main score part.
        assertTrue("Score was not extracted correctly by combined selector. Got: " + score, score.startsWith("Sri Lanka 178/7"));
    }
    
    @Test
    public void testGetLiveScoreOfSelectedMatch_Success_FallbackSelector() {
        // Selector 4 (Generic text pattern): "div[class*='score']" containing "150/2"
        String mockMatchPageHtml = "<html><body>" +
            "<div class='some-other-info'>Match Delayed</div>" +
            "<div class='team-a-score-panel'>" + // class contains 'score'
            "  Team A Current Score: 150/2 (20.0 overs)" +
            "</div>" +
            "</body></html>";
        
        String matchUrl = "https://www.espncricinfo.com/live-cricket-scores/series-fallback/match-fb";
        String score = CricinfoLive.getLiveScoreOfSelectedMatch(matchUrl, mockMatchPageHtml);

        assertEquals("Score was not extracted correctly by fallback selector", "Team A Current Score: 150/2 (20.0 overs)", score);
    }


    @Test
    public void testGetLiveScoreOfSelectedMatch_ScoreNotFound() {
        String mockMatchPageHtml = "<html><body>" +
            "<h1>Match Preview</h1>" +
            "<p>This match will start soon. No live score data available yet.</p>" +
            "<div>Info about teams</div>" +
            "</body></html>";
        
        String matchUrl = "https://www.espncricinfo.com/live-cricket-scores/series-preview/match-preview";
        String score = CricinfoLive.getLiveScoreOfSelectedMatch(matchUrl, mockMatchPageHtml);

        assertEquals("Default 'Score not available' should be returned", "Score not available", score);
    }
    
    @Test
    public void testGetLiveScoreOfSelectedMatch_ScoreNotFound_StatusFallback() {
        // Test the final fallback to "p.ds-text-tight-s.ds-font-regular.ds-line-clamp-2.ds-text-typo"
        String mockMatchPageHtml = "<html><body>" +
            "<h1>Match Information</h1>" +
            "<p class='ds-text-tight-s ds-font-regular ds-line-clamp-2 ds-text-typo'>Stumps - Day 1: Team Alpha trail by 100 runs</p>" +
            "<div>Some other details</div>" +
            "</body></html>";
        
        String matchUrl = "https://www.espncricinfo.com/live-cricket-scores/series-status/match-stumps";
        String score = CricinfoLive.getLiveScoreOfSelectedMatch(matchUrl, mockMatchPageHtml);

        assertEquals("Match status should be returned as fallback", "Stumps - Day 1: Team Alpha trail by 100 runs", score);
    }

    @Test
    public void testGetLiveScoreOfSelectedMatch_MalformedHtmlScore() {
        // HTML where score-like elements are present but don't fit the exact expected text pattern or are incomplete.
        String mockMatchPageHtml = "<html><body>" +
            "<div class='ds-text-compact-m ds-text-typo-title ds-text-right ds-whitespace-nowrap'>" +
            "  TeamOnly" + // No score numbers
            "</div>" +
            "<div class='current-score-class'>Wickets: 5</div>" + // Not the "SCORE/WICKETS" pattern
            "<p class='ds-text-tight-s ds-font-regular ds-line-clamp-2 ds-text-typo'>Match Abandoned due to rain.</p>" + // Fallback status
            "</body></html>";
        
        String matchUrl = "https://www.espncricinfo.com/live-cricket-scores/series-malformed/match-malformed";
        String score = CricinfoLive.getLiveScoreOfSelectedMatch(matchUrl, mockMatchPageHtml);

        // Expecting the fallback status because other selectors won't find a valid score.
        assertEquals("Fallback status should be returned for malformed score elements", "Match Abandoned due to rain.", score);
    }

    @Test
    public void testGetLiveScoreOfSelectedMatch_NullOrEmptyUrl() {
        String score1 = CricinfoLive.getLiveScoreOfSelectedMatch(null, "<html></html>");
        assertEquals("Score not available for null URL", "Score not available", score1);

        String score2 = CricinfoLive.getLiveScoreOfSelectedMatch("", "<html></html>");
        assertEquals("Score not available for empty URL", "Score not available", score2);
    }
}
