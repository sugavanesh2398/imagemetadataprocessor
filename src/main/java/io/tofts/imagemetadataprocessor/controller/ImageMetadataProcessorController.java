package io.tofts.imagemetadataprocessor.controller;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@RestController
public class ImageMetadataProcessorController {
    HashMap<String, List<String>> metadata=new HashMap<>();

    @PostMapping("/imagemetadataprocessor")
    public HashMap<String, List<String>> GetImageMetadata(@RequestParam("file") MultipartFile filePath) throws IOException, ImageProcessingException, JSONException {

        System.out.println("%%%%%%%%%%");
        File convFile = new File( filePath.getOriginalFilename() );
        FileOutputStream fos = new FileOutputStream( convFile );
        fos.write( filePath.getBytes() );
        fos.close();
        System.out.println("%%%%%%%%%%"+convFile.getName());
        Metadata meta= ImageMetadataReader.readMetadata(convFile);
        for (Directory directory : meta.getDirectories()){
            System.out.println("  directory=" + directory);
            List<String> listTag=new ArrayList<>();
            for (Tag tag : directory.getTags()) {
                System.out.println("    tag=" + tag);
                listTag.add(tag.toString());
            }
            metadata.put(directory.toString(),listTag);
        }

        return metadata;
    }
}
