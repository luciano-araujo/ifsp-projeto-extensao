package br.edu.ifsp.tcc.application.dto;

import jakarta.validation.constraints.Size;
import br.edu.ifsp.tcc.application.entity.LessonType;
import lombok.Data;

@Data
public class UpdateCourseLessonDTO {
    @Size(max = 255, message = "O título da aula deve ter no máximo 255 caracteres.")
    private String title;
    private LessonType type;
    private Boolean published;
    private Long projectId;
}
