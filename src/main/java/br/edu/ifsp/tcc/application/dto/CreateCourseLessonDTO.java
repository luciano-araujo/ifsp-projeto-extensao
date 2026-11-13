package br.edu.ifsp.tcc.application.dto;

import br.edu.ifsp.tcc.application.entity.LessonType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCourseLessonDTO {
    @NotBlank(message = "O título da aula é obrigatório.")
    @Size(max = 255, message = "O título da aula deve ter no máximo 255 caracteres.")
    private String title;

    private LessonType type = LessonType.VIDEO;

    /** Optional link to the team card that produces this lesson. */
    private Long projectId;

    @Size(max = 5000, message = "A descrição da aula deve ter no máximo 5000 caracteres.")
    private String description;

    /** http or https link; an empty text clears it (on update). */
    @Size(max = 500, message = "O link da aula deve ter no máximo 500 caracteres.")
    @Pattern(regexp = "(?i)^(\\s*|\\s*https?://[^\\s/\\p{Cntrl}]+[^\\s\\p{Cntrl}]*\\s*)$", message = "O link da aula deve começar com http:// ou https://.")
    private String contentUrl;
}
