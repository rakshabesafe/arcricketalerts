package com.vuzix.ultralite.sample;

public class MatchDetails {

    private final String matchTitle;
    private final String matchUrl;
    private final String score;

    public MatchDetails(String matchTitle, String matchUrl, String score) {
        this.matchTitle = matchTitle;
        this.matchUrl = matchUrl;
        this.score = score;
    }

    public String getMatchTitle() {
        return matchTitle;
    }

    public String getMatchUrl() {
        return matchUrl;
    }

    public String getScore() {
        return score;
    }
}
