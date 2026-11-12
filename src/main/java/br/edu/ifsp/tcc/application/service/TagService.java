package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.dto.TagResponse;
import br.edu.ifsp.tcc.application.entity.Tag;
import br.edu.ifsp.tcc.application.entity.Team;
import br.edu.ifsp.tcc.application.repository.TagRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Data access, lookups and mapping of the team tags. Business rules live in the tag use cases. */
@Service
public class TagService {

    /** Tags every team starts with (name to color). */
    public static final Map<String, String> DEFAULT_TAGS = defaultTags();

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    private static Map<String, String> defaultTags() {
        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("Tutorial", "#2E7D32");
        tags.put("Podcast", "#1565C0");
        tags.put("Curso", "#F57C00");
        tags.put("Roteiro", "#7B1FA2");
        tags.put("Artigo", "#C2185B");
        return tags;
    }

    public List<Tag> findTags(Long teamId) {
        return tagRepository.findByTeam_IdOrderByIdAsc(teamId);
    }

    public Tag findTag(Long teamId, Long tagId) {
        return tagRepository.findByIdAndTeam_Id(tagId, teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Etiqueta não encontrada."));
    }

    /**
     * Tag a card or idea asks for: by id (preferred) or, during the transition, by name inside the team.
     * Returns null when neither is given; an id or name that does not belong to the team is a 400.
     */
    public Tag resolveTag(Long teamId, Long tagId, String tagName) {
        if (tagId != null) {
            return tagRepository.findByIdAndTeam_Id(tagId, teamId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Etiqueta inválida para esta equipe."));
        }
        if (tagName == null || tagName.isBlank()) return null;
        return tagRepository.findByTeam_IdAndNameIgnoreCase(teamId, tagName.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Etiqueta \"" + tagName.trim() + "\" não existe nesta equipe."));
    }

    /** True when another tag of the team already uses the name (case-insensitive). */
    public boolean nameTaken(Long teamId, String name, Long exceptTagId) {
        return tagRepository.findByTeam_IdAndNameIgnoreCase(teamId, name)
                .filter(existing -> !existing.getId().equals(exceptTagId))
                .isPresent();
    }

    public Tag save(Tag tag) {
        return tagRepository.save(tag);
    }

    public void delete(Tag tag) {
        tagRepository.delete(tag);
    }

    /** Gives a new team the default tags. */
    public void createDefaultTags(Team team) {
        DEFAULT_TAGS.forEach((name, color) -> {
            Tag tag = new Tag();
            tag.setTeam(team);
            tag.setName(name);
            tag.setColor(color);
            tagRepository.save(tag);
        });
    }

    public TagResponse toTagResponse(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName(), tag.getColor());
    }
}
