package br.edu.ifsp.tcc.application.dto;

import jakarta.validation.constraints.Size;
import br.edu.ifsp.tcc.application.entity.LessonType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCourseLessonDTO {
    @NotBlank(message = "O titulo da aula e obrigatorio.")
    @Size(max = 255, message = "O título da aula deve ter no máximo 255 caracteres.")
    private String title;

    private LessonType type = LessonType.VIDEO;

    /** Optional link to the team card that produces this lesson. */
    private Long projectId;
}
