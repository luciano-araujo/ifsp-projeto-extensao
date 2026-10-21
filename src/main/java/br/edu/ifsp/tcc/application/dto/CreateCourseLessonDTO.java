package br.edu.ifsp.tcc.application.dto;

import br.edu.ifsp.tcc.application.entity.LessonType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCourseLessonDTO {
    @NotBlank(message = "O titulo da aula e obrigatorio.")
    private String title;

    private LessonType type = LessonType.VIDEO;

    /** Optional link to the team card that produces this lesson. */
    private Long projectId;
}
