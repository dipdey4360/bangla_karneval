package com.bangla.karneval.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class GalleryUploadRequest {
    @NotNull
    private Integer eventYear;
    private String       mediaType    = "IMAGE";
    private String       caption;
    private Boolean      isHighlight  = false;
    private Integer      displayOrder = 0;
    private MultipartFile file;
    private String       url;
}
