package br.edu.ifsp.tcc.application.controller;

import br.edu.ifsp.tcc.application.dto.CreateTagDTO;
import br.edu.ifsp.tcc.application.dto.TagResponse;
import br.edu.ifsp.tcc.application.dto.UpdateTagDTO;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.usecase.CreateTagUseCase;
import br.edu.ifsp.tcc.application.usecase.DeleteTagUseCase;
import br.edu.ifsp.tcc.application.usecase.ListTagsUseCase;
import br.edu.ifsp.tcc.application.usecase.UpdateTagUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TagControllerTest {

    @Mock private ListTagsUseCase listTagsUseCase;
    @Mock private CreateTagUseCase createTagUseCase;
    @Mock private UpdateTagUseCase updateTagUseCase;
    @Mock private DeleteTagUseCase deleteTagUseCase;
    @InjectMocks private TagController controller;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
    }

    @Test
    void list_shouldReturnTags() {
        List<TagResponse> tags = List.of(new TagResponse(1L, "Tutorial", "#2E7D32"));
        when(listTagsUseCase.execute(10L, 1L)).thenReturn(tags);

        ResponseEntity<List<TagResponse>> response = controller.list(user, 10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(tags, response.getBody());
    }

    @Test
    void create_shouldReturnCreatedTag() {
        CreateTagDTO dto = new CreateTagDTO();
        TagResponse created = new TagResponse(2L, "Live", "#112233");
        when(createTagUseCase.execute(10L, 1L, dto)).thenReturn(created);

        ResponseEntity<TagResponse> response = controller.create(user, 10L, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(created, response.getBody());
    }

    @Test
    void update_shouldReturnUpdatedTag() {
        UpdateTagDTO dto = new UpdateTagDTO();
        TagResponse updated = new TagResponse(2L, "Live", "#112233");
        when(updateTagUseCase.execute(10L, 1L, 2L, dto)).thenReturn(updated);

        ResponseEntity<TagResponse> response = controller.update(user, 10L, 2L, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(updated, response.getBody());
    }

    @Test
    void delete_shouldReturnNoContent() {
        ResponseEntity<Void> response = controller.delete(user, 10L, 2L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(deleteTagUseCase).execute(10L, 1L, 2L);
    }
}
