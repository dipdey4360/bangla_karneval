package com.bangla.karneval.service;

import com.bangla.karneval.dto.request.GalleryUploadRequest;
import com.bangla.karneval.model.EventConfig;
import com.bangla.karneval.model.GalleryItem;
import com.bangla.karneval.repository.EventConfigRepository;
import com.bangla.karneval.repository.GalleryItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GalleryService {

    @Autowired private GalleryItemRepository galleryItemRepository;
    @Autowired private EventConfigRepository  eventConfigRepository;
    @Autowired private ProgrammeService programmes;

    private static final String UPLOAD_DIR = "/app/static/assets/images/gallery/";

    public List<GalleryItem> getByYear(Integer year) {
        return galleryItemRepository.findByEventEditionIdOrderByDisplayOrderAsc(programmes.legacyId(year));
    }

    public List<GalleryItem> getHighlights(Integer year) {
        return getByYear(year).stream().filter(i->Boolean.TRUE.equals(i.getIsHighlight())).toList();
    }

    /* Years from event_config — used by ADMIN upload dropdown (valid years only) */
    public List<Integer> getAvailableYears() {
        return eventConfigRepository.findAllByOrderByEventYearDesc()
                .stream()
                .map(EventConfig::getEventYear)
                .collect(Collectors.toList());
    }

    /* Years from gallery_items — used by PUBLIC gallery (years with actual images) */
    public List<Integer> getGalleryYears() {
        return galleryItemRepository.findDistinctEventYears();
    }

    @Transactional
    public GalleryItem upload(GalleryUploadRequest request) throws IOException {

        var edition=programmes.get(programmes.resolve(request.getEventEditionId(),request.getEventYear()));
        String fileUrl = request.getUrl();
        if (request.getFile() != null && !request.getFile().isEmpty()) {
            fileUrl = saveFile(request.getFile(), edition.getEventYear());
        }

        GalleryItem item = new GalleryItem();
        item.setEventYear(edition.getEventYear());
        item.setEventEditionId(edition.getEventEditionId());
        item.setMediaType(request.getMediaType());
        item.setUrl(fileUrl);
        item.setCaption(request.getCaption());
        item.setIsHighlight(request.getIsHighlight());
        item.setDisplayOrder(request.getDisplayOrder());
        return galleryItemRepository.save(item);
    }

    @Transactional
    public GalleryItem toggleHighlight(Long id) {
        GalleryItem item = galleryItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Gallery item not found"));
        item.setIsHighlight(!item.getIsHighlight());
        return galleryItemRepository.save(item);
    }

    @Transactional
    public void delete(Long id) {
        galleryItemRepository.deleteById(id);
    }

    private String saveFile(MultipartFile file, Integer year) throws IOException {
        String dir = UPLOAD_DIR + year + "/";
        Files.createDirectories(Paths.get(dir));
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path path = Paths.get(dir + filename);
        Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
        return "/assets/images/gallery/" + year + "/" + filename;
    }
}
