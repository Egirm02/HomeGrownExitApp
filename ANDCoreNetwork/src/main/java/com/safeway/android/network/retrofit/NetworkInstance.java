package com.safeway.android.network.retrofit;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.net.Proxy;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class NetworkInstance {

    private final static OkHttpClient client = new OkHttpClient();

    public static class Builder {

        private String baseUrl;
        private Proxy proxy;
        private long readTimeout = 0l; //in ms
        private long connectTimeout = 0l; //in ms
        private boolean withLogging = false;
        private String tag="OkHttp";

        public Builder(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public Builder setProxy(Proxy proxy) {
            this.proxy = proxy;
            return this;
        }

        public Builder setReadTimeout(long readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public Builder setConnectTimeout(long connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder isWithLogging(boolean withLogging) {
            this.withLogging = withLogging;
            return this;
        }

        public Builder setTag(String tag) {
            this.tag=tag;
            return this;
        }

        public OkHttpClient createOkHttpClient() {
            OkHttpClient.Builder httpClient = client.newBuilder();
            if(proxy != null) {
                httpClient.proxy(proxy);
            }

            if(readTimeout > 0l) {
                httpClient.readTimeout(connectTimeout, TimeUnit.MILLISECONDS);
            }

            if(connectTimeout > 0l) {
                httpClient.connectTimeout(connectTimeout, TimeUnit.MILLISECONDS);
            }
            if(withLogging) {

                HttpLoggingInterceptor logging = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
                    @Override
                    public void log(String message) {
                        Log.d(tag, message, null);
                    }
                });

////
////            // set your desired log level
                logging.setLevel(HttpLoggingInterceptor.Level.BODY);
                httpClient.addInterceptor(logging);
            }
            return httpClient.build();
        }

        public Retrofit build() {

            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();



            Retrofit.Builder builder = new Retrofit.Builder()
                    .baseUrl(baseUrl==null ? "http://www.safeway.com" : baseUrl)
                    .addConverterFactory(NullOnEmptyConverterFactory.create())
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .client(createOkHttpClient());

            return builder.build();
        }
    }

}