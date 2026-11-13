package br.edu.ifsp.tcc.application.dto;

import br.edu.ifsp.tcc.application.entity.CourseStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class CourseResponse {
    private Long id;
    private String uuid;
    private Long teamId;
    private String title;
    private String description;
    private CourseStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime publishedAt;

    private Integer modulesCount;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime createdAt;
}
