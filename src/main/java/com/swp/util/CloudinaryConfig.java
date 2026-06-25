package com.swp.util;

import com.cloudinary.Cloudinary;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

public class CloudinaryConfig {
    private static final Cloudinary cloudinary;

    static {
        try {
            Properties props = new Properties();
            InputStream input = CloudinaryConfig.class
                    .getClassLoader()
                    .getResourceAsStream("cloudinary.properties");
            if (input == null) {
                throw new RuntimeException("Cannot find cloudinary.properties");
            }
            props.load(input);
            cloudinary = new Cloudinary(Map.of(
                    "cloud_name", props.getProperty("cloudinary.cloud_name"),
                    "api_key", props.getProperty("cloudinary.api_key"),
                    "api_secret", props.getProperty("cloudinary.api_secret"),
                    "secure", true
            ));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load Cloudinary config", e);
        }
    }

    public static Cloudinary getCloudinary() {
        return cloudinary;
    }
}