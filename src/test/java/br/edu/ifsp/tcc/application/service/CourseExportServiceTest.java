package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.dto.CourseDetailResponse;
import br.edu.ifsp.tcc.application.dto.CourseLessonResponse;
import br.edu.ifsp.tcc.application.dto.CourseModuleResponse;
import br.edu.ifsp.tcc.application.dto.MaterialResponse;
import br.edu.ifsp.tcc.application.entity.CourseStatus;
import br.edu.ifsp.tcc.application.entity.LessonType;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CourseExportServiceTest {

    private final CourseExportService service = new CourseExportService();

    private MaterialResponse material(String name) {
        return new MaterialResponse(1L, "u", 2L, name, "application/pdf", 3L, null, null, "/x");
    }

    private CourseLessonResponse lesson(String title, String url, Long projectId, List<MaterialResponse> materials) {
        return new CourseLessonResponse(1L, "u", 2L, title, null, url, LessonType.VIDEO, true, 0, projectId, null, materials);
    }

    @Test
    void toCsv_shouldWriteHeaderAndOneRowPerLessonWithEscaping() {
        CourseModuleResponse m1 = new CourseModuleResponse(1L, "u", 9L, "Introdução", 0, List.of(
                lesson("Aula; um", "https://youtu.be/dQw4w9WgXcQ", 7L, List.of(material("a.pdf"), material("b.pdf"))),
                lesson("Diz \"oi\"", null, null, List.of())), null);
        CourseModuleResponse m2 = new CourseModuleResponse(2L, "u", 9L, "=Vazio", 1, List.of(), null);
        CourseDetailResponse tree = new CourseDetailResponse(9L, "u", 1L, "C", null, CourseStatus.DRAFT, null, 2, List.of(m1, m2), null);

        String csv = new String(service.toCsv(tree), StandardCharsets.UTF_8);

        assertEquals(CourseExportService.CSV_HEADER + "\r\n"
                + "1;Introdução;1;\"Aula; um\";VIDEO;true;https://youtu.be/dQw4w9WgXcQ;7;a.pdf | b.pdf\r\n"
                + "1;Introdução;2;\"Diz \"\"oi\"\"\";VIDEO;true;;;\r\n"
                + "2;'=Vazio;;;;;;;\r\n", csv);
    }

    @Test
    void toCsv_shouldWriteOnlyTheHeaderForACourseWithoutModules() {
        CourseDetailResponse tree = new CourseDetailResponse(9L, "u", 1L, "C", null, CourseStatus.DRAFT, null, 0, List.of(), null);

        assertEquals(CourseExportService.CSV_HEADER + "\r\n", new String(service.toCsv(tree), StandardCharsets.UTF_8));
    }
}
