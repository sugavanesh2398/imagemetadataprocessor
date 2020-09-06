package io.tofts.imagemetadataprocessor.controller;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.internal.InputSubstream;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.servlet.ServletContext;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/io/tofts")
public class ImageMetadataProcessorController {
    HashMap<String, List<String>> metadata=new HashMap<>();
    @Autowired
    public ServletContext context;

    @PostMapping("/imagemetadataprocessor")
    public HashMap<String, List<String>> GetImageMetadata(@RequestParam("file") MultipartFile filePath) throws IOException, ImageProcessingException, JSONException {
        BasicAWSCredentials creds = new BasicAWSCredentials("AKIAWH4BFBSOXX2FT5TE", "2dNR42V1bQYsK0YRfxirXkBAU49o2XYXJIAa7q0u");

        AmazonS3Client s3 = new AmazonS3Client(creds);
        System.out.println("%%%%%%%%%%");
        File convFile = new File( filePath.getOriginalFilename() );
        FileOutputStream fos = new FileOutputStream( convFile );
        fos.write( filePath.getBytes() );
        fos.close();
        System.out.println("%%%%%%%%%%"+convFile.getName());
        s3.putObject("image-store-metadata",convFile.getName(),convFile);
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

    @PostMapping("/convertfile")
    public ResponseEntity FileConverter(@RequestParam("file") MultipartFile inputFile, @RequestParam("format") String format) throws IOException{
        File convFile = new File( inputFile.getOriginalFilename() );
        System.out.println(convFile.getName()+"------"+inputFile.getOriginalFilename());
        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            fos.write(inputFile.getBytes());
            fos.close();
        }
        //MediaType mediaType = MediaTypeUtils.getMediaTypeForFileName(this.servletContext, fileName);
        String mineType = context.getMimeType(convFile.getName());
        MediaType mediaType = MediaType.parseMediaType(mineType);
        //MediaType mediaType = MediaTypeUtils.getMediaTypeForFileName(this.servletContext, fileName);

        File outputFile = new File("test."+format);

        String []str=outputFile.getName().split(".");
        System.out.println(str.length+"----");
        try (InputStream is = new FileInputStream(convFile)) {
            ImageInputStream iis = ImageIO.createImageInputStream(is);
            BufferedImage image = ImageIO.read(iis);
            try (OutputStream os = new FileOutputStream(outputFile)) {
                ImageOutputStream ios = ImageIO.createImageOutputStream(os);
                ImageIO.write(image, format, ios);

            } catch (Exception exp) {
                exp.printStackTrace();
            }
        } catch (Exception exp) {
            exp.printStackTrace();
        }
        System.out.println(outputFile.getName());
        InputStreamResource resource = new InputStreamResource(new FileInputStream(outputFile));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + outputFile.getName())
                .contentType(mediaType)
                .body(resource);
    }
}
