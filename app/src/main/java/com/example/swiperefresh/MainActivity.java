package com.example.swiperefresh;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class MainActivity extends AppCompatActivity {

    private SwipeRefreshLayout refreshLayout;
    private ArrayList<String> title;
    private ArrayList<String> pubDate;
    private ArrayList<String> description;
    private ArrayList<String> url;
    private ListView feeds;
    private ArrayList<ListViewItems> lvArray;
    private String defaultUrl;
    private String selectedURL;
    private ListViewItemsAdapter lvAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        defaultUrl = "https://www.winnipegfreepress.com/rss";

        selectedURL = defaultUrl;

        processRss(selectedURL);

        //get the actual refresh layout, layout
        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.refreshLayout);

        //set the color scheme for the actual reloading symbol. You can add as many colors as you want
        refreshLayout.setColorSchemeColors(Color.RED, Color.GREEN);

        //this is for setting the background size
        refreshLayout.setProgressBackgroundColorSchemeColor(Color.BLACK);

        //there are two sizes, Large and Default
        refreshLayout.setSize(SwipeRefreshLayout.LARGE);


        //set the on refresh listener, so that when it is refreshed it reprocesses the feed.
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //start the refreshing animation, set it to false when processing is done to stop the animation.
                refreshLayout.setRefreshing(true);

                processRss(selectedURL);
            }
        });

    }

    public void processRss(String url){
        Toast.makeText(this, "Processing Rss", Toast.LENGTH_LONG).show();
        RssProcessor rssFeed = new RssProcessor(url);
        rssFeed.execute();
    }
    class RssProcessor extends AsyncTask {
        String rssURL;
        public RssProcessor(String url){
            this.rssURL = url;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Object doInBackground(Object[] objects) {

            URL rssUrl = null;
            try {
                rssUrl = new URL(rssURL);

            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            SAXParser saxParser = null;
            try{
                saxParser = SAXParserFactory.newInstance().newSAXParser();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            }

            StbHandler stbHandler = new StbHandler();

            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection)rssUrl.openConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                saxParser.parse(connection.getInputStream(), stbHandler);
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        private void makeLVArray() {
            lvArray = new ArrayList<>();

            for(int i = 0; i < 10; i ++){
                lvArray.add(new ListViewItems(title.get(i), pubDate.get(i), description.get(i)));
            }
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);

            feeds = (ListView)findViewById(R.id.listview);
            lvAdapter = new ListViewItemsAdapter(MainActivity.this, R.id.listview, lvArray );
            feeds.setAdapter(lvAdapter);

            //Notifies the attached observers that the underlying data has been changed and any View reflecting the data set should refresh itself.
            lvAdapter.notifyDataSetChanged();
        }

        class StbHandler extends DefaultHandler {

            private boolean inTitle, inPubDate, inDescription, inItem, inUrl;
            StringBuilder builder;
            {
                title = new ArrayList<>();
                pubDate = new ArrayList<>();
                description = new ArrayList<>();
                url =  new ArrayList<>();
            }

            @Override
            public void startDocument() throws SAXException {
                super.startDocument();
            }

            @Override
            public void endDocument() throws SAXException {
                super.endDocument();
                makeLVArray();

                //this is where we stop the refreshing animation
                refreshLayout.setRefreshing(false);
            }

            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                super.startElement(uri, localName, qName, attributes);
                builder = new StringBuilder();
                if(qName.equals("item")){
                    inItem = true;
                } else if(qName.equals("title") && inItem) {
                    inTitle = true;
                } else if (qName.equals("pubDate") && inItem){
                    inPubDate = true;
                } else if (qName.equals("description") && inItem){
                    inDescription = true;
                } else if (qName.equals("link")){
                    inUrl = true;
                }

            }

            @Override
            @TargetApi(24)
            public void endElement(String uri, String localName, String qName) throws SAXException {
                super.endElement(uri, localName, qName);
                if(qName.equals("title") && inItem) {
                    inTitle = false;
                    title.add(builder.toString());
                } else if (qName.equals("pubDate") && inItem){
                    pubDate.add(builder.toString());
                    inPubDate = false;
                } else if (qName.equals("description") && inItem){
                    String desc = Html.fromHtml(builder.toString(), Html.FROM_HTML_SEPARATOR_LINE_BREAK_PARAGRAPH).toString();
                    desc = desc.substring(0, 75);
                    desc = desc.concat(" ...");
                    description.add(desc);
                    inDescription = false;
                } else if (qName.equals("link") && inItem){
                    url.add(builder.toString());
                    inUrl = false;
                }
            }

            @Override
            public void characters(char[] ch, int start, int length) throws SAXException {
                super.characters(ch, start, length);

                String s = new String(ch, start, length);

                if(inTitle){
                    builder.append(s);
                } else if (inPubDate){
                    builder.append(s);
                } else if (inDescription){
                    builder.append(s);
                } else if (inUrl){
                    builder.append(s);
                }
            }

            @Override
            public void warning(SAXParseException e) throws SAXException {
                super.warning(e);
            }

            @Override
            public void error(SAXParseException e) throws SAXException {
                super.error(e);
            }

            @Override
            public void fatalError(SAXParseException e) throws SAXException {
                super.fatalError(e);
            }


        }
    }

    class ListViewItems {
        private String title;
        private String pubDate;
        private String description;

        private ListViewItems(String title, String pubDate, String description)
        {
            this.title = title;
            this.pubDate = pubDate;
            this.description = description;
        }

        private String getTitle() {return title;}
        private String getPubDate() {return pubDate;}
        private String getDescription() {return description;}

    }

    private class ListViewItemsAdapter extends ArrayAdapter<ListViewItems> {

        private ArrayList<ListViewItems> items;

        public ListViewItemsAdapter(Context context, int textViewResourceId, ArrayList<ListViewItems> items) {
            super(context, textViewResourceId, items);
            this.items = items;
        }

        //This method is called once for every item in the ArrayList as the list is loaded.
        //It returns a View -- a list item in the ListView -- for each item in the ArrayList
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.rowlayout, null);
            }
            ListViewItems o = items.get(position);
            if (o != null) {
                TextView title =  v.findViewById(R.id.tvTitle);
                TextView pubDate = v.findViewById(R.id.tvPubDate);
                TextView description = v.findViewById(R.id.tvDescription);
                if (title != null) {
                    title.setText("Title: " + o.getTitle());
                }
                if (pubDate != null) {
                    pubDate.setText("PubDate: " + o.getPubDate());
                }
                if (description != null) {
                    description.setText("Description: " + o.getDescription());
                }
            }
            return v;
        }
    }



}
