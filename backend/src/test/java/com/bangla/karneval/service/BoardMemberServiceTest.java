package com.bangla.karneval.service;
import com.bangla.karneval.repository.BoardMemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class BoardMemberServiceTest {
    @TempDir Path directory;
    @Test void rejectsInvalidPositionsAndNonImages() {
        var service=new BoardMemberService(mock(BoardMemberRepository.class),directory.toString());
        assertThrows(ResponseStatusException.class,()->service.save(7,"Name","President",null,false));
        var html=new MockMultipartFile("image","photo.png","image/png","<script>bad</script>".getBytes());
        assertThrows(ResponseStatusException.class,()->service.save(1,"Name","President",html,false));
    }
    @Test void reencodesImageAndIgnoresUserFilename() throws Exception {
        var repository=mock(BoardMemberRepository.class);
        when(repository.save(any())).thenAnswer(i->i.getArgument(0));
        var service=new BoardMemberService(repository,directory.toString());
        var data=new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(4,4,BufferedImage.TYPE_INT_RGB),"png",data);
        var image=new MockMultipartFile("image","../../unsafe.png","image/png",data.toByteArray());
        var saved=service.save(1,"Alice","President",image,false);
        assertTrue(saved.getImageUrl().matches("/uploads/board/[a-f0-9-]+\\.png"));
        try(var files=Files.list(directory)) { assertEquals(1,files.count()); }
    }
}
