package com.bangla.karneval.service;

import com.bangla.karneval.model.BoardMember;
import com.bangla.karneval.repository.BoardMemberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;

@Service
public class ProgrammePosterService {
    private final Path directory;
    public ProgrammePosterService(@Value("${app.poster-upload-dir:/app/uploads/posters}") String directory) { this.directory=Paths.get(directory); }
    public String saveImage(MultipartFile upload) throws IOException {
        if (upload.getSize() > 5 * 1024 * 1024) throw new ResponseStatusException(BAD_REQUEST, "Image must be 5 MB or smaller");
        BufferedImage decoded;
        try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(upload.getBytes()))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new ResponseStatusException(BAD_REQUEST, "Upload a JPEG or PNG image");
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName();
                if (!format.equalsIgnoreCase("JPEG") && !format.equalsIgnoreCase("PNG"))
                    throw new ResponseStatusException(BAD_REQUEST, "Upload a JPEG or PNG image");
                reader.setInput(input);
                if ((long) reader.getWidth(0) * reader.getHeight(0) > 16_000_000)
                    throw new ResponseStatusException(BAD_REQUEST, "Image must be smaller than 16 megapixels");
                decoded = reader.read(0);
            } finally { reader.dispose(); }
        } catch (IOException e) {
            throw new ResponseStatusException(BAD_REQUEST, "The image could not be read", e);
        }
        Files.createDirectories(directory);
        String filename = UUID.randomUUID() + ".png";
        // Re-encode pixels, never serve arbitrary uploaded file content or filenames.
        ImageIO.write(decoded, "png", directory.resolve(filename).toFile());
        return "/uploads/posters/" + filename;
    }
}
