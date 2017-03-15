package fr.pchab.androidrtc.data;

import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by duongnx on 3/14/2017.
 */

public class WebService extends AsyncTask<String, String, String> {

    public interface OnServerListener {
        void onServerComplete(ArrayList<User> users);

        void onServerFailed();
    }

    private OnServerListener onServerListener;
    private ArrayList<User> users = null;

    public WebService(OnServerListener listener) {
        onServerListener = listener;
    }

    @Override
    protected String doInBackground(String... params) {
        HttpURLConnection httpURLConnection = null;
        try {
            URL url = new URL(params[0]);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setRequestProperty("Content-length", "0");
            httpURLConnection.setConnectTimeout(60000);
            httpURLConnection.setReadTimeout(60000);

            httpURLConnection.connect();
            int responseCode = httpURLConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line + "\n");
                }
                br.close();
                Log.d("duongnx", "" + sb.toString());
                Gson gson = new Gson();
                users = gson.fromJson(sb.toString(), new TypeToken<ArrayList<User>>() {
                }.getType());
            }


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (httpURLConnection != null)
                httpURLConnection.disconnect();
        }
        return null;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        if (onServerListener != null)
            onServerListener.onServerComplete(users);
    }
}
