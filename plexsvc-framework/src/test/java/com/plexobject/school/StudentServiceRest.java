package com.plexobject.school;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.plexobject.handler.Request;
import com.plexobject.handler.RequestHandler;
import com.plexobject.service.RequestMethod;
import com.plexobject.service.Protocol;
import com.plexobject.service.ServiceConfig;

public class StudentServiceRest {
    private static Map<String, Student> students = new HashMap<>();

    @ServiceConfig(protocol = Protocol.HTTP, payloadClass = Student.class, endpoint = "/students", method = RequestMethod.POST)
    public static class SaveHandler implements RequestHandler {
        @Override
        public void handle(Request request) {
            Student student = request.getPayload();
            students.put(student.getId(), student);
            request.getResponse().setPayload(student);
        }
    }

    @ServiceConfig(protocol = Protocol.HTTP, payloadClass = Void.class, endpoint = "/students", method = RequestMethod.GET)
    public static class QueryHandler implements RequestHandler {
        @Override
        public void handle(Request request) {
            String studentId = request.getStringProperty("studentId");
            String courseId = request.getStringProperty("courseId");
            List<Student> list = new ArrayList<>();

            for (Student student : students.values()) {
                if (studentId != null && student.getId().equals(studentId)) {
                    list.add(student);
                } else if (courseId != null
                        && student.getCourseIds().contains(courseId)) {
                    list.add(student);
                }
            }
            request.getResponse().setPayload(list);
        }
    }

    @ServiceConfig(protocol = Protocol.HTTP, payloadClass = Void.class, endpoint = "/students/{studentId}", method = RequestMethod.GET)
    public static class GetHandler implements RequestHandler {
        @Override
        public void handle(Request request) {
            String studentId = request.getStringProperty("studentId");
            Student s = students.get(studentId);

            if (s == null) {
                throw new IllegalArgumentException("student not found for "
                        + studentId + ", local " + students.keySet());
            }
            request.getResponse().setPayload(s);
        }
    }

}