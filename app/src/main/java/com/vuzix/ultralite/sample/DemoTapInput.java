package com.vuzix.ultralite.sample;

import android.content.Context;

import com.vuzix.ultralite.EventListener;
import com.vuzix.ultralite.LVGLImage;
import com.vuzix.ultralite.Layout;
import com.vuzix.ultralite.UltraliteSDK;
import com.vuzix.ultralite.utils.scroll.LiveText;
import com.vuzix.ultralite.utils.scroll.TextToImageSlicer;

import java.util.List;
import java.util.regex.Pattern;

/**
 * This class demonstrates using tap input from the glasses.
 */
public class DemoTapInput {
    final static int sliceHeight = 60; // Height of each slice of text (including inter-line padding)
    final static int fontSize = 48;    // Font size within one slice of text (smaller than the sliceHeight
    final static int lowestLineShowing = 3;
    final static int maxLinesShowing = 2;
    final static int fastScrollMilliSecs = 500;

    final static Pattern pattern = Pattern.compile("india|chennai|mumbai|rajasthan|gujarat|lucknow|royal|delhi|kolkata|netherlands", Pattern.CASE_INSENSITIVE);


    private static void chunkStringsToEngine(MainActivity.DemoActivityViewModel demoActivityViewModel, LiveText liveTextSender, int intervalMs, List<String> fullStrings) throws MainActivity.Stop {
        String fullTextToSend = "";
        for (String eachLine : fullStrings) {
            if(pattern.matcher(eachLine).find()) {
                continue;
            }
            // We append lines together to simulate the results of a speech engine. It will give us a partial
            // result, then update that over and over again, growing and changing the text as it goes.
            // As long as we use one LiveText class, it will manage this properly. So, for the demo, we
            // just send a block of text (with no correlation to screen lines) and let the LiveText break
            // it into lines and show what it needs to.
            fullTextToSend += eachLine + " ";
            liveTextSender.sendText(fullTextToSend);
            // We pause as we parse the text array to simulate the speech engine giving us data over time
            demoActivityViewModel.pause(intervalMs);
        }
    }

    public static void runDemo(Context context, MainActivity.DemoActivityViewModel demoActivityViewModel, UltraliteSDK ultralite) throws MainActivity.Stop {
        final int SCREEN_TIMEOUT_SECS = 15;
        final boolean HIDE_STATUS_BAR = false;
        final int maxTaps = 2;
        boolean animateTaps = true;
        CricinfoLive cricInfo = new CricinfoLive();

        final int sliceHeightInPixels = 48;    // The lines will be 48 pixels high, so each line is 1/10th the screen height. This affects the
        // ranges for all other values below since this configuration now has a maximum of 10 lines.
        final int sliceWidthInPixels = UltraliteSDK.Canvas.WIDTH; // Use the full width
        final int startingScreenLocation = 1;  // The lines will appear at line 0, the lowest point on the screen.  (Since above we
        // configured a total of 10 lines on the screen, this can be (0-9) and we're choosing 1.
        final int numberLinesShowing = 3;      // Number of full lines when the text pauses. A fourth line shows during the transition.
        // (Since each line is set to be 48 pixels high above, we can have a max of 10 lines on
        // the screen, 1 up from the bottom, we can choose between 1 and 9, and we choose 3).
        ultralite.setLayout(Layout.SCROLL, SCREEN_TIMEOUT_SECS, HIDE_STATUS_BAR, animateTaps, maxTaps);
        UltraliteSDK.ScrollingTextView scrollingTextView = ultralite.getScrollingTextView();
        scrollingTextView.scrollLayoutConfig(sliceHeight, lowestLineShowing, maxLinesShowing, fastScrollMilliSecs, false);

        LiveText liveTextSender = new LiveText(ultralite, sliceHeightInPixels, sliceWidthInPixels, startingScreenLocation, numberLinesShowing, null);
        liveTextSender.sendText(context.getString(R.string.tap_once));


        // We need to add an event listener if we want to know when the taps occur
        TapListener tapListener = new TapListener();
        ultralite.addEventListener(tapListener);

        int numTaps;
        do {
            numTaps = tapListener.waitForTaps();
            if(numTaps == 1) {
                try {
                    List<String> scores=CricinfoLive.fetchLiveScores();
                    liveTextSender.sendText(scores.get(CricinfoLive.selectedMatchIndex));
                    //chunkStringsToEngine(demoActivityViewModel, liveTextSender, 2000, scores);
                } catch (Exception e) {
                    e.printStackTrace();

                }

            }
        } while (numTaps != 2);

        demoActivityViewModel.pause(2000);
        // Unregister this so our listener stops being called
        ultralite.removeEventListener(tapListener);
    }

    // The event listener has many indications, this one is occurs when the frames are touched
    static class TapListener implements EventListener {
        private volatile int userTapCount;

        @Override
        public void onTap(int tapCount) {
            synchronized (this) {
                userTapCount = tapCount;
                this.notifyAll();
            }
        }

        // This method blocks the calling thread until the taps are received
        public synchronized int waitForTaps() throws MainActivity.Stop {
            userTapCount = 0;
            try {
                this.wait();   // Block the thread here
            } catch (InterruptedException e) {
                throw new MainActivity.Stop(true);
            }
            return userTapCount;
        }
    };
}
