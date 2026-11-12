package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.dto.TagResponse;
import br.edu.ifsp.tcc.application.entity.Tag;
import br.edu.ifsp.tcc.application.entity.Team;
import br.edu.ifsp.tcc.application.repository.TagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock private TagRepository tagRepository;
    @InjectMocks private TagService service;

    private Tag tag(Long id, String name) {
        Tag tag = new Tag();
        tag.setId(id);
        tag.setName(name);
        tag.setColor("#2E7D32");
        return tag;
    }

    @Test
    void findTags_shouldReturnTagsOfTheTeam() {
        List<Tag> tags = List.of(tag(1L, "Tutorial"));
        when(tagRepository.findByTeam_IdOrderByIdAsc(10L)).thenReturn(tags);

        assertSame(tags, service.findTags(10L));
    }

    @Test
    void findTag_shouldReturnTagOfTheTeam() {
        Tag tutorial = tag(1L, "Tutorial");
        when(tagRepository.findByIdAndTeam_Id(1L, 10L)).thenReturn(Optional.of(tutorial));

        assertSame(tutorial, service.findTag(10L, 1L));
    }

    @Test
    void findTag_shouldReturnNotFoundForTagOfAnotherTeam() {
        when(tagRepository.findByIdAndTeam_Id(1L, 10L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.findTag(10L, 1L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void resolveTag_shouldPreferTheIdOverTheName() {
        Tag byId = tag(1L, "Tutorial");
        when(tagRepository.findByIdAndTeam_Id(1L, 10L)).thenReturn(Optional.of(byId));

        assertSame(byId, service.resolveTag(10L, 1L, "Podcast"));
        verify(tagRepository, never()).findByTeam_IdAndNameIgnoreCase(any(), any());
    }

    @Test
    void resolveTag_shouldRejectIdOfAnotherTeam() {
        when(tagRepository.findByIdAndTeam_Id(9L, 10L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.resolveTag(10L, 9L, null));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void resolveTag_shouldFindByNameInsideTheTeamDuringTheTransition() {
        Tag podcast = tag(2L, "Podcast");
        when(tagRepository.findByTeam_IdAndNameIgnoreCase(10L, "podcast")).thenReturn(Optional.of(podcast));

        assertSame(podcast, service.resolveTag(10L, null, " podcast "));
    }

    @Test
    void resolveTag_shouldRejectUnknownName() {
        when(tagRepository.findByTeam_IdAndNameIgnoreCase(10L, "Inexistente")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.resolveTag(10L, null, "Inexistente"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Inexistente"));
    }

    @Test
    void resolveTag_shouldReturnNullWithoutIdAndWithBlankName() {
        assertNull(service.resolveTag(10L, null, null));
        assertNull(service.resolveTag(10L, null, "   "));
        verifyNoInteractions(tagRepository);
    }

    @Test
    void nameTaken_shouldIgnoreTheTagBeingEdited() {
        Tag podcast = tag(2L, "Podcast");
        when(tagRepository.findByTeam_IdAndNameIgnoreCase(10L, "podcast")).thenReturn(Optional.of(podcast));
        when(tagRepository.findByTeam_IdAndNameIgnoreCase(10L, "Livre")).thenReturn(Optional.empty());

        assertTrue(service.nameTaken(10L, "podcast", null));
        assertTrue(service.nameTaken(10L, "podcast", 3L));
        assertFalse(service.nameTaken(10L, "podcast", 2L));
        assertFalse(service.nameTaken(10L, "Livre", null));
    }

    @Test
    void saveAndDelete_shouldDelegateToRepository() {
        Tag tutorial = tag(1L, "Tutorial");
        when(tagRepository.save(tutorial)).thenReturn(tutorial);

        assertSame(tutorial, service.save(tutorial));
        service.delete(tutorial);

        verify(tagRepository).delete(tutorial);
    }

    @Test
    void createDefaultTags_shouldSeedTheFiveDefaultsForTheTeam() {
        Team team = new Team();
        team.setId(10L);

        service.createDefaultTags(team);

        ArgumentCaptor<Tag> saved = ArgumentCaptor.forClass(Tag.class);
        verify(tagRepository, times(5)).save(saved.capture());
        assertEquals(List.of("Tutorial", "Podcast", "Curso", "Roteiro", "Artigo"),
                saved.getAllValues().stream().map(Tag::getName).toList());
        assertEquals("#2E7D32", saved.getAllValues().get(0).getColor());
        assertEquals("#C2185B", saved.getAllValues().get(4).getColor());
        assertTrue(saved.getAllValues().stream().allMatch(t -> t.getTeam() == team));
    }

    @Test
    void toTagResponse_shouldExposeIdNameAndColor() {
        TagResponse response = service.toTagResponse(tag(1L, "Tutorial"));

        assertEquals(1L, response.getId());
        assertEquals("Tutorial", response.getName());
        assertEquals("#2E7D32", response.getColor());
    }
}
