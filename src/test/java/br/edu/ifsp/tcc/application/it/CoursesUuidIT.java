package br.edu.ifsp.tcc.application.it;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static br.edu.ifsp.tcc.application.it.Api.map;
import static org.junit.jupiter.api.Assertions.*;

/** RN05 on courses, modules and lessons, plus the team isolation of the (unchanged) course routes. */
class CoursesUuidIT extends AbstractIT {

    @Test
    @DisplayName("IT-RN05 Course, module and lesson carry a distinct uuid in every response")
    void uuidsOnCourseTree() {
        Team team = newTeam();
        String base = team.path() + "/courses";
        Resp course = api.post(base, ana.token(), map("title", "Curso", "description", "d"));
        assertEquals(200, course.status(), course.body());
        long courseId = course.num("id");
        Resp module = api.post(base + "/" + courseId + "/modules", ana.token(), map("title", "Modulo 1"));
        assertEquals(200, module.status(), module.body());
        Resp lesson = api.post(base + "/" + courseId + "/modules/" + module.num("id") + "/lessons", ana.token(),
                map("title", "Aula 1", "type", "VIDEO"));
        assertEquals(200, lesson.status(), lesson.body());

        Set<String> uuids = new HashSet<>();
        for (String uuid : new String[]{course.text("uuid"), module.text("uuid"), lesson.text("uuid")}) {
            assertNotNull(uuid);
            assertDoesNotThrow(() -> UUID.fromString(uuid));
            assertTrue(uuids.add(uuid), "uuid repeated");
        }
        Resp detail = api.get(base + "/" + courseId, rui.token());
        assertEquals(200, detail.status(), detail.body());
        assertEquals(course.text("uuid"), detail.text("uuid"));
        assertEquals(module.text("uuid"), detail.text("modules.0.uuid"));
        assertEquals(lesson.text("uuid"), detail.text("modules.0.lessons.0.uuid"));
        assertEquals(1, api.get(base, rui.token()).size());
        assertNotNull(api.get(base, rui.token()).text("0.uuid"));
    }

    @Test
    @DisplayName("SEC-COURSE-01 Another team cannot read or delete the course (403 at team level, 404 by id)")
    void courseIsolation() {
        Team team = newTeam();
        long courseId = api.post(team.path() + "/courses", ana.token(), map("title", "Privado")).num("id");
        assertEquals(403, api.get(team.path() + "/courses/" + courseId, bia.token()).status());
        assertEquals(404, api.get(otherTeam.path() + "/courses/" + courseId, bia.token()).status());
        assertEquals(404, api.delete(otherTeam.path() + "/courses/" + courseId, bia.token()).status());
        assertEquals(401, api.get(team.path() + "/courses/" + courseId, null).status());
        assertEquals(200, api.get(team.path() + "/courses/" + courseId, ana.token()).status());
    }
}
