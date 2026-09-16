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
public class BoardMemberService {
    private final BoardMemberRepository repository;
    private final Path directory;
    public BoardMemberService(BoardMemberRepository repository,
            @Value("${app.board-upload-dir:/app/uploads/board}") String directory) {
        this.repository = repository;
        this.directory = Paths.get(directory);
    }
    public List<BoardMember> list() { return repository.findAllByOrderByIdAsc(); }

    @Transactional
    public BoardMember save(int slot, String name, String designation, MultipartFile image, boolean removeImage) throws IOException {
        checkSlot(slot);
        if (name == null || name.isBlank() || name.length() > 100 || designation == null || designation.isBlank() || designation.length() > 100)
            throw new ResponseStatusException(BAD_REQUEST, "Name and designation are required (maximum 100 characters)");
        BoardMember member = repository.findById(slot).orElseGet(BoardMember::new);
        member.setId(slot);
        member.setName(name.trim());
        member.setDesignation(designation.trim());
        if (removeImage) member.setImageUrl(null);
        if (image != null && !image.isEmpty()) member.setImageUrl(saveImage(image));
        return repository.save(member);
    }
    @Transactional
    public void clear(int slot) { checkSlot(slot); repository.deleteById(slot); }
    private void checkSlot(int slot) {
        if (slot < 1 || slot > 6) throw new ResponseStatusException(BAD_REQUEST, "Choose a board position from 1 to 6");
    }
    private String saveImage(MultipartFile upload) throws IOException {
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
        return "/uploads/board/" + filename;
    }
}
