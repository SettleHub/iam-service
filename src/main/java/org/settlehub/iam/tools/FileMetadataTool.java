package org.settlehub.iam.tools;

import org.settlehub.iam.tools.models.Metadata;
import org.springframework.web.multipart.MultipartFile;

public class FileMetadataTool {

    public static Metadata getFrom(MultipartFile multipartFile, Long userId) {
        Metadata data = new Metadata();
        data.setFilename(sanitizeFilename(multipartFile.getOriginalFilename()));
        data.setFileExtension(
            data.getFilename().substring(data.getFilename().lastIndexOf('.') + 1)
        );
        data.setMimeType(multipartFile.getContentType());
        data.setCreatedAt(DateTool.getCurrentDate());
        return data;
    }

    /**
     * Sanitizes the filename by removing potentially dangerous characters.
     * Removes harmful characters and paths (e.g., "..", "/", "\")
     *
     * @param filename the original file name
     * @return a safe file name
     */
    private static String sanitizeFilename(String filename) {
        if (filename == null) {
            throw new IllegalArgumentException("Filename cannot be null");
        }
        return filename.replaceAll("[\\\\/]", "").trim();
    }

    public static String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return ""; // no extension found
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}