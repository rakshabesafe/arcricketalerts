package com.vuzix.ultralite.sample;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class CricinfoLive {
    private static final String TAG = "CricinfoFetcher";
    private static final String CRICINFO_RSS_URL = "https://static.cricinfo.com/rss/livescores.xml";

    protected static String selectedMatchTitle="India";

    protected static int selectedMatchIndex=0;


    /**
     * Fetches live cricket scores from the Cricinfo RSS feed.
     *
     * @return A list of strings, where each string represents a live score item, or null in case of error
     */
    public static List<String> fetchLiveScores() {
        List<String> scores = new ArrayList<>();
        try {
            String xmlData = fetchXmlData(CRICINFO_RSS_URL);
            if (xmlData != null) {
                scores = parseXml(xmlData);
            }
        } catch (IOException | ParserConfigurationException | SAXException e) {
            Log.e(TAG, "Error fetching or parsing RSS feed: " + e.getMessage());
            // Consider throwing the exception or returning a specific error value/object.
            //  Don't just return null, handle the error as appropriate for your application.
            return new ArrayList<String>(); // returning null for error
        }
        return scores;
    }

    /**
     * Fetches XML data from the given URL.
     *
     * @param urlString The URL to fetch the XML from.
     * @return The XML data as a string, or null on error.
     * @throws IOException if an error occurs during the network operation.
     */
    private static String fetchXmlData(String urlString) throws IOException {
        InputStream is = null;
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP error code: " + responseCode);
            }
            is = conn.getInputStream();
            return readIt(is);
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    /**
     * Reads an InputStream and converts it to a String.
     *
     * @param stream The InputStream to read.
     * @return The content of the InputStream as a String.
     * @throws IOException if an error occurs during reading.
     */
    private static String readIt(InputStream stream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append('\n');
        }
        return sb.toString();
    }

    /**
     * Parses the XML data to extract the live scores.
     *
     * @param xmlData The XML data as a string.
     * @return A list of strings, where each string represents a live score item.
     * @throws ParserConfigurationException if a DocumentBuilder cannot be created.
     * @throws IOException                  if an error occurs during parsing.
     * @throws SAXException                   if an error occurs during parsing.
     */
    private static List<String> parseXml(String xmlData) throws ParserConfigurationException, IOException, SAXException {
        List<String> scores = new ArrayList<>();
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        InputSource inputSource = new InputSource(new StringReader(xmlData)); // Use InputSource
        Document doc = dBuilder.parse(inputSource);  // Parse from InputSource
        doc.getDocumentElement().normalize();

        NodeList nList = doc.getElementsByTagName("item");
        for (int temp = 0; temp < nList.getLength(); temp++) {
            Node nNode = nList.item(temp);
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                Element eElement = (Element) nNode;
                String title = eElement.getElementsByTagName("title").item(0).getTextContent();
                scores.add(title);
            }
        }
        return scores;
    }
}
