package br.edu.ifsp.tcc.application.dto;

import br.edu.ifsp.tcc.application.entity.LessonType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateLessonDTO {
    @NotBlank
    private String title;
    private LessonType type = LessonType.VIDEO;
}
