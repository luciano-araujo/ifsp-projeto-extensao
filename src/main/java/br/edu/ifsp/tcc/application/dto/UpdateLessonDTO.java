package br.edu.ifsp.tcc.application.dto;

import br.edu.ifsp.tcc.application.entity.LessonType;
import lombok.Data;

@Data
public class UpdateLessonDTO {
    private String title;
    private LessonType type;
    private Boolean published;
    private Integer sortOrder;
}
