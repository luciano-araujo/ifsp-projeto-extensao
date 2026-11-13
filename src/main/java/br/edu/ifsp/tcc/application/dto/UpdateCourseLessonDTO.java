package br.edu.ifsp.tcc.application.dto;

import br.edu.ifsp.tcc.application.entity.LessonType;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateCourseLessonDTO {
    @Size(max = 255, message = "O título da aula deve ter no máximo 255 caracteres.")
    private String title;
    private LessonType type;
    private Boolean published;
    private Long projectId;

    @Size(max = 5000, message = "A descrição da aula deve ter no máximo 5000 caracteres.")
    private String description;

    /** http or https link; an empty text clears it (on update). */
    @Size(max = 2048, message = "O link da aula deve ter no máximo 2048 caracteres.")
    @Pattern(regexp = "(?i)^(https?://[^\\s/]+\\S*)?$", message = "O link da aula deve começar com http:// ou https://.")
    private String contentUrl;
}
