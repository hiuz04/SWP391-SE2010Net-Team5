package com.swp.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class RecaptchaUtil {

    private static final String RECAPTCHA_URL = "https://www.google.com/recaptcha/api/siteverify";

    public static boolean verify(String recaptchaResponse) {
        if (recaptchaResponse == null || recaptchaResponse.isEmpty()) {
            return false;
        }

        try {
            URL url = new URL(RECAPTCHA_URL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

            String postParams = "secret=" + RecaptchaConfig.getSecretKey() + "&response=" + recaptchaResponse;

            con.setDoOutput(true);
            try (DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
                wr.writeBytes(postParams);
                wr.flush();
            }

            int responseCode = con.getResponseCode();
            if (responseCode != 200) {
                return false;
            }

            StringBuilder responseStr = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    responseStr.append(inputLine);
                }
            }

            // Parse JSON response simply
            String json = responseStr.toString();
            return json.contains("\"success\": true");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
