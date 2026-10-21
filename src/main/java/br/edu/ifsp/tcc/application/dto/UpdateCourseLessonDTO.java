package br.edu.ifsp.tcc.application.dto;

import br.edu.ifsp.tcc.application.entity.LessonType;
import lombok.Data;

@Data
public class UpdateCourseLessonDTO {
    private String title;
    private LessonType type;
    private Boolean published;
    private Long projectId;
}
