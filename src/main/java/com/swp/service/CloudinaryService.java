package com.swp.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.swp.model.dto.CloudinaryResponse;
import com.swp.util.CloudinaryConfig;

import java.io.IOException;
import java.util.Map;

public class CloudinaryService {

    private static final Cloudinary cloudinary = CloudinaryConfig.getCloudinary();

    public CloudinaryResponse upload(byte[] data) throws IOException {

        Map<?, ?> result = cloudinary.uploader().upload(
                data,
                ObjectUtils.emptyMap()
        );

        return new CloudinaryResponse(
                result.get("secure_url").toString(),
                result.get("public_id").toString()
        );
    }

    public void delete(String publicId) throws IOException {

        cloudinary.uploader().destroy(
                publicId,
                ObjectUtils.emptyMap()
        );
    }

}
