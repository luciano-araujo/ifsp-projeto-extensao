package br.edu.ifsp.tcc.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** A downloadable export: the body is a byte array (CSV) or the course tree (JSON). */
@Data
@AllArgsConstructor
public class CourseExportFile {
    private String fileName;
    private String contentType;
    private Object body;
}
