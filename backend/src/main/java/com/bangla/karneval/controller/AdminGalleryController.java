package com.bangla.karneval.controller;

import com.bangla.karneval.dto.request.GalleryUploadRequest;
import com.bangla.karneval.dto.response.ApiResponse;
import com.bangla.karneval.model.GalleryItem;
import com.bangla.karneval.service.GalleryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/admin/gallery")
public class AdminGalleryController {

    @Autowired private GalleryService galleryService;

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<GalleryItem> upload(
            @RequestParam Integer      eventYear,
            @RequestParam(required = false) MultipartFile file,
            @RequestParam(required = false) String        url,
            @RequestParam(defaultValue = "IMAGE") String  mediaType,
            @RequestParam(required = false) String        caption,
            @RequestParam(defaultValue = "false") Boolean isHighlight,
            @RequestParam(defaultValue = "0") Integer     displayOrder) throws IOException {

        GalleryUploadRequest request = new GalleryUploadRequest();
        request.setEventYear(eventYear);
        request.setFile(file);
        request.setUrl(url);
        request.setMediaType(mediaType);
        request.setCaption(caption);
        request.setIsHighlight(isHighlight);
        request.setDisplayOrder(displayOrder);
        return ResponseEntity.ok(galleryService.upload(request));
    }

    @PutMapping("/{id}/highlight")
    public ResponseEntity<GalleryItem> toggleHighlight(@PathVariable Long id) {
        return ResponseEntity.ok(galleryService.toggleHighlight(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id) {
        galleryService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Gallery item deleted"));
    }

    @GetMapping("/years")
    public ResponseEntity<List<Integer>> getAvailableYears() {
        return ResponseEntity.ok(galleryService.getAvailableYears());
    }
}
