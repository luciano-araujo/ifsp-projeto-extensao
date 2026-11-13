package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.dto.CourseDetailResponse;
import br.edu.ifsp.tcc.application.dto.CourseLessonResponse;
import br.edu.ifsp.tcc.application.dto.CourseModuleResponse;
import br.edu.ifsp.tcc.application.dto.MaterialResponse;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** CSV rendering of a course tree: UTF-8, ';' separator, one row per lesson. */
@Service
public class CourseExportService {

    public static final String CSV_HEADER =
            "modulo_ordem;modulo_titulo;aula_ordem;aula_titulo;aula_tipo;aula_publicada;aula_link;projeto_id;materiais";

    /** UTF-8 byte order mark, so Excel opens the accents correctly. */
    public static final String BOM = "﻿";

    public byte[] toCsv(CourseDetailResponse course) {
        StringBuilder csv = new StringBuilder(BOM).append(CSV_HEADER).append("\r\n");
        int moduleOrder = 0;
        for (CourseModuleResponse module : course.getModules()) {
            moduleOrder++;
            if (module.getLessons().isEmpty()) {
                csv.append(row(List.of(String.valueOf(moduleOrder), text(module.getTitle()), "", "", "", "", "", "", "")))
                        .append("\r\n");
                continue;
            }
            int lessonOrder = 0;
            for (CourseLessonResponse lesson : module.getLessons()) {
                lessonOrder++;
                List<String> cells = new ArrayList<>();
                cells.add(String.valueOf(moduleOrder));
                cells.add(text(module.getTitle()));
                cells.add(String.valueOf(lessonOrder));
                cells.add(text(lesson.getTitle()));
                cells.add(String.valueOf(lesson.getType()));
                cells.add(String.valueOf(Boolean.TRUE.equals(lesson.getPublished())));
                cells.add(lesson.getContentUrl() == null ? "" : lesson.getContentUrl());
                cells.add(lesson.getProjectId() == null ? "" : String.valueOf(lesson.getProjectId()));
                cells.add(lesson.getMaterials().stream().map(MaterialResponse::getFileName).map(this::text)
                        .collect(Collectors.joining(" | ")));
                csv.append(row(cells)).append("\r\n");
            }
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String row(List<String> cells) {
        return cells.stream().map(this::escape).collect(Collectors.joining(";"));
    }

    /** Typed text that a spreadsheet would run as a formula gets a leading apostrophe. */
    private String text(String value) {
        if (value == null) return "";
        return !value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0)) >= 0 ? "'" + value : value;
    }

    private String escape(String cell) {
        if (cell.contains(";") || cell.contains("\"") || cell.contains("\n") || cell.contains("\r")) {
            return "\"" + cell.replace("\"", "\"\"") + "\"";
        }
        return cell;
    }
}
