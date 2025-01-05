package com.example.thymeleaf;

import com.example.thymeleaf.entity.Student;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StudentTest {


    private void invokePrePersist(Student student) {
        try {
            Method prePersist = Student.class.getDeclaredMethod("prePersist");
            prePersist.setAccessible(true);
            prePersist.invoke(student);
        } catch (NoSuchMethodException
                 | IllegalAccessException
                 | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }


    private Student createAndPersistStudent(String name, String email, LocalDate birthday) {
        Student student = new Student();
        student.setName(name);
        student.setEmail(email);
        student.setBirthday(birthday);

        invokePrePersist(student);

        return student;
    }

    @Test
    void correctInputTest() {
        List<TestData> inputs = Arrays.asList(
                new TestData("Jan Kowalski", "jkowalski@test.com", LocalDate.of(2000, 1, 1)),
                new TestData("John Doe", "john.doe@example.org", LocalDate.of(1999, 6, 9)),
                new TestData("Marek Marek", "X".repeat(64) + "@gogle.com", LocalDate.of(1950, 12, 13))
        );

        inputs.forEach(data ->
                assertDoesNotThrow(() -> createAndPersistStudent(data.name, data.email, data.birthday)));
    }

    @Test
    void incorrectInputTest() {
        List<TestData> inputs = Arrays.asList(
                new TestData(null, "null@example.com", LocalDate.of(2000, 1, 1)),
                new TestData("", "", LocalDate.of(2000, 2, 1)),
                new TestData("   ", "   @test.com", LocalDate.of(2000, 3, 1)),
                new TestData("John", "john@@example.com", LocalDate.of(2001, 1, 1)),
                new TestData("A", "john.doe\n@example.com", LocalDate.of(2001, 2, 2)),
                new TestData("D", "john..doe@hospital.com", LocalDate.of(2002, 1, 1)),
                new TestData("\t", "   . c o m", LocalDate.of(2005, 1, 3)),
                new TestData("Mareek", "marek@marek.pl", LocalDate.of(2225, 12, 31))
        );

        inputs.forEach(data -> assertThrows(RuntimeException.class, () -> createAndPersistStudent(data.name, data.email, data.birthday)));
    }

    @Test
    void incorrectInputTest_XSS() {
        List<TestData> inputs = Arrays.asList(
                new TestData("<script>alert('1')</script>", "test@xss.com", LocalDate.of(2000, 1, 1)),
                new TestData("XSS Attack", "<script>...@com", LocalDate.of(2000, 2, 1)),
                new TestData("IMG", "<img/src/onerror=prompt(5)", LocalDate.of(2000, 3, 1)),
                new TestData("XSS prompt", "'-prompt(5)'", LocalDate.of(2000, 4, 1))
        );

        inputs.forEach(data -> assertThrows(RuntimeException.class, () -> createAndPersistStudent(data.name, data.email, data.birthday)));

    }

    @Test
    void incorrectInputTest_SQLi() {
        List<TestData> inputs = Arrays.asList(
                new TestData("Marek'); DROP TABLE student;--", "sql@example.com", LocalDate.of(2000, 1, 1)),
                new TestData("SQLi Test", "email@\" OR 1=1 --.com", LocalDate.of(2000, 2, 1)),
                new TestData("Union Test", "' UNION SELECT sum(...) from tablename --", LocalDate.of(2000, 3, 1)),
                new TestData("-- or # ", "test@test.com", LocalDate.of(2000, 4, 1))
        );

        inputs.forEach(data -> assertThrows(RuntimeException.class, () -> createAndPersistStudent(data.name, data.email, data.birthday)));

    }


    @Test
    void extremeInputTest() {
        List<TestData> inputs = Arrays.asList(
                new TestData("X".repeat(10_000), "x10000@example.com", LocalDate.of(2000, 1, 1)),
                new TestData("X".repeat(100_000), "x100000@example.com", LocalDate.of(2000, 2, 1)),
                new TestData("X".repeat(400_000), "x400000@example.com", LocalDate.of(2000, 3, 1)),
                new TestData("Rafal Wo", "X".repeat(10_000) + "@onragee.com", LocalDate.of(2001, 1, 1)),
                new TestData("Rafal Woo", "X".repeat(100_000) + "@onragee.com", LocalDate.of(2001, 2, 1)),
                new TestData("Rafal Wooo", "X".repeat(400_000) + "@onragee.com", LocalDate.of(2001, 3, 1))
        );

        inputs.forEach(data -> assertThrows(RuntimeException.class, () -> createAndPersistStudent(data.name, data.email, data.birthday)));

    }

    static class TestData {
        String name;
        String email;
        LocalDate birthday;

        TestData(String name, String email, LocalDate birthday) {
            this.name = name;
            this.email = email;
            this.birthday = birthday;
        }
    }
}