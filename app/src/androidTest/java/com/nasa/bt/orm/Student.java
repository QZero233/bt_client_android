package com.nasa.bt.orm;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "student")
public class Student {

    @DatabaseField(id = true)
    private String id;
    @DatabaseField(useGetSet = true)
    private String name;
    @DatabaseField(useGetSet = true)
    private int age;
    @DatabaseField(useGetSet = true)
    private long birthDate;
    @DatabaseField(useGetSet = true)
    private String title;

    public Student() {
    }

    public Student(String id, String name, int age, long birthDate) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.birthDate = birthDate;
    }

    public Student(String id, String name, int age, long birthDate, String title) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.birthDate = birthDate;
        this.title = title;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public long getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(long birthDate) {
        this.birthDate = birthDate;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String toString() {
        return "Student{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", age=" + age +
                ", birthDate=" + birthDate +
                ", title='" + title + '\'' +
                '}';
    }
}
