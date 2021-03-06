package io.tofts.imagemetadataprocessor.controller;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import springfox.documentation.annotations.ApiIgnore;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

@RestController
public class ImageMetadataProcessorController {
    HashMap<String, List<String>> metadata=new HashMap<>();
    @Autowired
    public ServletContext context;
    @Autowired
    private HttpServletRequest request;

    @RequestMapping(value = "/")
    @ApiIgnore
    public void redirect(HttpServletResponse response) throws IOException {
        response.sendRedirect("/swagger-ui.html");
    }

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



    @PostMapping("/convertfile")
    public ResponseEntity FileConverter(@RequestParam("file") MultipartFile inputFile, @RequestParam("format") String format) throws IOException{

        File convFile = new File( inputFile.getOriginalFilename());
        convFile.createNewFile();
        System.out.println("------");
        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            fos.write(inputFile.getBytes());
            fos.close();
        }
        DateTime time=new DateTime();
        System.out.println(time.toString());
        String mineType = context.getMimeType(convFile.getName());
        MediaType mediaType = MediaType.parseMediaType(mineType);
        File outputFile = new File(time.getDayOfYear()+time.getMillis()+"test."+format);
        try (InputStream is = new FileInputStream(convFile)) {
            ImageInputStream iis = ImageIO.createImageInputStream(is);
            BufferedImage image = ImageIO.read(convFile); //change 1
            BufferedImage newBufferedImage = new BufferedImage(image.getWidth(), //change 2
                    image.getHeight(), BufferedImage.TYPE_INT_RGB);
            newBufferedImage.createGraphics().drawImage(image, 0, 0, Color.WHITE, null);

            try (OutputStream os = new FileOutputStream(outputFile)) {
                ImageOutputStream ios = ImageIO.createImageOutputStream(os);
                ImageIO.write(newBufferedImage, format, ios);  // change3

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

    @PostMapping("/imagecompression")
    public ResponseEntity ImageCompression(@RequestParam MultipartFile inputFile) throws IOException {
         System.out.println(inputFile.getOriginalFilename());
          if(!inputFile.getContentType().split("/")[1].equalsIgnoreCase("PNG")){
            File convFile = new File(inputFile.getOriginalFilename());
            //convFile.createNewFile();   //
            System.out.println("------");
            try (FileOutputStream fos = new FileOutputStream(convFile)) {
                fos.write(inputFile.getBytes());
                fos.close();
            }
            DateTime time = new DateTime();
            System.out.println(time.toString());
            BufferedImage image = ImageIO.read(convFile); //change 1
            System.out.println("^^^^^^" + convFile.getName());
            File compressedImageFile = new File(time.getDayOfYear()+time.getMillis() + ".jpg"); //
            OutputStream os = new FileOutputStream(compressedImageFile);

            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg"); //
            ImageWriter writer = (ImageWriter) writers.next();

            ImageOutputStream ios = ImageIO.createImageOutputStream(os);
            writer.setOutput(ios);

            ImageWriteParam param = writer.getDefaultWriteParam();

            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.20f);
            writer.write(null, new IIOImage(image, null, null), param);

            os.close();
            ios.close();
            writer.dispose();
            String mineType = context.getMimeType(convFile.getName());
            MediaType mediaType = MediaType.parseMediaType(mineType);

            InputStreamResource resource = new InputStreamResource(new FileInputStream(compressedImageFile));
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + compressedImageFile.getName())
                    .contentType(mediaType)
                    .body(resource);
        }
        else {
            return ResponseEntity.ok("PNG can't be compressed");
        }

    }

}
