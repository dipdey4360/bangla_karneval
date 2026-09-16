package com.bangla.karneval.service;

import com.bangla.karneval.model.Sponsor;
import com.bangla.karneval.repository.SponsorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

@Service
public class SponsorService {

    @Autowired
    private SponsorRepository sponsorRepository;

    @Value("${app.upload.dir:/app/uploads/sponsors}")
    private String uploadDir;

    public List<Sponsor> getAllForAdmin() {
        return sponsorRepository.findAllByOrderByDisplayOrderAsc();
    }

    public List<Sponsor> getVisibleSponsors() {
        return sponsorRepository.findByIsVisibleTrueOrderByDisplayOrderAsc();
    }

    @Transactional
    public Sponsor createSponsor(String name, String address, String websiteUrl,
                                 String phone, String description,
                                 MultipartFile logo) throws IOException {
        Sponsor sponsor = new Sponsor();
        sponsor.setName(name);
        sponsor.setAddress(address);
        sponsor.setWebsiteUrl(websiteUrl);
        sponsor.setPhone(phone);
        sponsor.setDescription(description);
        sponsor.setDisplayOrder(sponsorRepository.findAllByOrderByDisplayOrderAsc().size());
        if (logo != null && !logo.isEmpty()) sponsor.setLogoPath(saveLogo(logo));
        return sponsorRepository.save(sponsor);
    }

    @Transactional
    public Sponsor updateSponsor(Long id, String name, String address, String websiteUrl,
                                 String phone, String description,
                                 MultipartFile logo) throws IOException {
        Sponsor sponsor = sponsorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sponsor not found"));
        sponsor.setName(name);
        sponsor.setAddress(address);
        sponsor.setWebsiteUrl(websiteUrl);
        sponsor.setPhone(phone);
        sponsor.setDescription(description);
        if (logo != null && !logo.isEmpty()) {
            if (sponsor.getLogoPath() != null) deleteLogo(sponsor.getLogoPath());
            sponsor.setLogoPath(saveLogo(logo));
        }
        return sponsorRepository.save(sponsor);
    }

    @Transactional
    public void toggleVisibility(Long id) {
        Sponsor sponsor = sponsorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sponsor not found"));
        sponsor.setIsVisible(!sponsor.getIsVisible());
        sponsorRepository.save(sponsor);
    }

    @Transactional
    public void updateOrder(List<Long> orderedIds) {
        for (int i = 0; i < orderedIds.size(); i++) {
            sponsorRepository.findById(orderedIds.get(i)).ifPresent(s -> {
                s.setDisplayOrder(orderedIds.indexOf(s.getId()));
                sponsorRepository.save(s);
            });
        }
    }

    @Transactional
    public void deleteSponsor(Long id) {
        Sponsor s = sponsorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sponsor not found"));
        if (s.getLogoPath() != null) deleteLogo(s.getLogoPath());
        sponsorRepository.deleteById(id);
    }

    private String saveLogo(MultipartFile file) throws IOException {
        Path dir = Paths.get(uploadDir);
        Files.createDirectories(dir);
        String filename = UUID.randomUUID() + "_" +
                file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_");
        Files.copy(file.getInputStream(), dir.resolve(filename),
                StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/sponsors/" + filename;
    }

    private void deleteLogo(String logoPath) {
        try {
            String filename = logoPath.replace("/uploads/sponsors/", "");
            Files.deleteIfExists(Paths.get(uploadDir).resolve(filename));
        } catch (IOException ignored) {}
    }
}
