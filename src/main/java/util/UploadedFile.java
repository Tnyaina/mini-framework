package util;

import jakarta.servlet.http.Part;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class UploadedFile {
    private String filename;
    private String contentType;
    private byte[] content;
    private Part part;
    
    public UploadedFile(String filename, String contentType, byte[] content, Part part) {
        this.filename = filename;
        this.contentType = contentType;
        this.content = content;
        this.part = part;
    }
    
    public String getFilename() {
        return filename;
    }
    
    public String getContentType() {
        return contentType;
    }
    
    public byte[] getContent() {
        return content;
    }
    
    public Part getPart() {
        return part;
    }
    
    public long getSize() {
        return content != null ? content.length : 0;
    }
    
    /**
     * Sauvegarde le fichier à un chemin donné
     */
    public void saveTo(String path) throws IOException {
        Path filePath = Paths.get(path);
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, content);
    }
    
    /**
     * Extrait le nom du fichier depuis le header Content-Disposition
     */
    public static String extractFilename(Part part) {
        String contentDisposition = part.getHeader("content-disposition");
        if (contentDisposition == null) return "unknown";
        
        for (String token : contentDisposition.split(";")) {
            if (token.trim().startsWith("filename")) {
                String filename = token.substring(token.indexOf('=') + 1).trim();
                return filename.replace("\"", "");
            }
        }
        return "unknown";
    }
}