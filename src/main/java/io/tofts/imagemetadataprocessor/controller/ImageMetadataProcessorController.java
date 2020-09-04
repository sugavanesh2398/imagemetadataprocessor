package io.tofts.imagemetadataprocessor.controller;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@RestController
public class ImageMetadataProcessorController {
    HashMap<String, List<String>> metadata=new HashMap<>();

    @RequestMapping("imagemetadataprocessor")
    @ResponseBody
    public String GetImageMetadata(@RequestBody String filePath) throws IOException, ImageProcessingException, JSONException {
        System.out.println("%%%%%%%%%%"+filePath);
        JSONObject json=new JSONObject(filePath);
        InputStream imageFile = new FileInputStream(json.getString("filePath"));
        Metadata meta= ImageMetadataReader.readMetadata(imageFile);
        for (Directory directory : meta.getDirectories()){
            System.out.println("  directory=" + directory);
            List<String> listTag=new ArrayList<>();
            for (Tag tag : directory.getTags()) {
                System.out.println("    tag=" + tag);
                listTag.add(tag.toString());
            }
            metadata.put(directory.toString(),listTag);
        }

        return filePath;
    }
}
