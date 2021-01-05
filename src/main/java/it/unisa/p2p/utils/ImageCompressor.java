/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unisa.p2p.utils;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/**
 *
 * @author raffaeledragone
 */
public class ImageCompressor {
    
    public static byte[] compressImageInJpeg(BufferedImage image, float quality) throws IOException {
        
        // The important part: Create in-memory stream
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();

        try (ImageOutputStream outputStream = ImageIO.createImageOutputStream(compressed)) {

            // NOTE: The rest of the code is just a cleaned up version of your code
            // Obtain writer for JPEG format
            ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName("JPEG").next();

            // Configure JPEG compression: 70% quality
            ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
            jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            jpgWriteParam.setCompressionQuality(quality);

            // Set your in-memory stream as the output
            jpgWriter.setOutput(outputStream);

            // Write image as JPEG w/configured settings to the in-memory stream
            // (the IIOImage is just an aggregator object, allowing you to associate
            // thumbnails and metadata to the image, it "does" nothing)
            jpgWriter.write(null, new IIOImage(image, null, null), jpgWriteParam);

            // Dispose the writer to free resources
            jpgWriter.dispose();
        }

// Get data for further processing...
        byte[] jpegData = compressed.toByteArray();
        return jpegData;
    }
    
    public static byte[] compressImageInJpeg(String pathToImg, float quality) throws IOException{
       File input = new File(pathToImg);
       BufferedImage image = ImageIO.read(input);
       return compressImageInJpeg(image, quality);
   }
    
    public static BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) throws IOException {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = resizedImage.createGraphics();
        graphics2D.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        graphics2D.dispose();
        return resizedImage;
    }
}
