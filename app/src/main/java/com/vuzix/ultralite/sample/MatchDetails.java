package com.vuzix.ultralite.sample;

public class MatchDetails {

    private final String matchTitle;
    private final String matchUrl;

    public MatchDetails(String matchTitle, String matchUrl) {
        this.matchTitle = matchTitle;
        this.matchUrl = matchUrl;
    }

    public String getMatchTitle() {
        return matchTitle;
    }

    public String getMatchUrl() {
        return matchUrl;
    }
}
