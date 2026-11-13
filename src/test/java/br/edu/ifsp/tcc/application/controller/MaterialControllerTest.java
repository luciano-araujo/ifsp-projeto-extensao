package br.edu.ifsp.tcc.application.controller;

import br.edu.ifsp.tcc.application.dto.MaterialDownload;
import br.edu.ifsp.tcc.application.dto.MaterialResponse;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.usecase.DeleteMaterialUseCase;
import br.edu.ifsp.tcc.application.usecase.DownloadMaterialUseCase;
import br.edu.ifsp.tcc.application.usecase.ListCardMaterialsUseCase;
import br.edu.ifsp.tcc.application.usecase.UploadMaterialUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaterialControllerTest {

    @Mock private UploadMaterialUseCase uploadMaterialUseCase;
    @Mock private ListCardMaterialsUseCase listCardMaterialsUseCase;
    @Mock private DownloadMaterialUseCase downloadMaterialUseCase;
    @Mock private DeleteMaterialUseCase deleteMaterialUseCase;
    @InjectMocks private MaterialController controller;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
    }

    @Test
    void upload_shouldReturnTheCreatedMaterial() {
        MockMultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", "abc".getBytes());
        MaterialResponse material = mock(MaterialResponse.class);
        when(uploadMaterialUseCase.execute(10L, 1L, 3L, file)).thenReturn(material);

        ResponseEntity<MaterialResponse> response = controller.upload(user, 10L, 3L, file);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(material, response.getBody());
    }

    @Test
    void list_shouldReturnTheMaterialsOfTheCard() {
        List<MaterialResponse> materials = List.of(mock(MaterialResponse.class));
        when(listCardMaterialsUseCase.execute(10L, 1L, 3L)).thenReturn(materials);

        ResponseEntity<List<MaterialResponse>> response = controller.list(user, 10L, 3L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(materials, response.getBody());
    }

    @Test
    void download_shouldStreamTheFileWithTypeLengthDispositionAndNosniff() {
        Resource resource = new ByteArrayResource("abc".getBytes());
        MaterialDownload file = new MaterialDownload(resource, "application/pdf", 3L,
                "attachment; filename=\"a.pdf\"; filename*=UTF-8''a.pdf");
        when(downloadMaterialUseCase.execute(10L, 1L, 7L, false)).thenReturn(file);

        ResponseEntity<Resource> response = controller.download(user, 10L, 7L, false);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(resource, response.getBody());
        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertEquals(3L, response.getHeaders().getContentLength());
        assertEquals(file.getContentDisposition(), response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        assertEquals("nosniff", response.getHeaders().getFirst("X-Content-Type-Options"));
    }

    @Test
    void download_shouldPassTheInlineFlag() {
        MaterialDownload file = new MaterialDownload(new ByteArrayResource(new byte[0]), "image/png", 0L, "inline; x");
        when(downloadMaterialUseCase.execute(10L, 1L, 7L, true)).thenReturn(file);

        ResponseEntity<Resource> response = controller.download(user, 10L, 7L, true);

        assertEquals("inline; x", response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
    }

    @Test
    void delete_shouldReturnNoContent() {
        ResponseEntity<Void> response = controller.delete(user, 10L, 7L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(deleteMaterialUseCase).execute(10L, 1L, 7L);
    }
}
