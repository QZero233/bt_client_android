package com.nasa.bt.orm;

import java.util.List;

public interface IStudentOperation {

    boolean addStudent(Student student);
    boolean updateStudent(Student oldStudent,Student newStudent);
    boolean deleteStudent(Student student);
    List<Student> getAllStudent();
    Student getStudentById(String id);
}
