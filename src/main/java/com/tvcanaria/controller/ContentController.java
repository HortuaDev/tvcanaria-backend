package com.tvcanaria.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


import com.tvcanaria.service.ContentService;

@RestController
@RequestMapping("/api/content")
public class ContentController {

    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    @PostMapping("/{id}/upload-video")
    public ResponseEntity<String> uploadVideo(@PathVariable Integer id,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        String url_video = contentService.submitVideo(id, file, authentication);

        if (url_video != null) {
            return ResponseEntity.ok(url_video);
        } else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteContent(@PathVariable Integer id, Authentication authentication) {

        try {
            if (contentService.deleteVideo(id, authentication)) {

                return ResponseEntity.ok("Artículo y video eliminados correctamente");
            } else {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al eliminar: " + e.getMessage());
        }
    }

}
