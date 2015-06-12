package com.plexobject.handler.javaws;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.jws.WebMethod;

public class CourseServiceClient extends BaseServiceClient implements
        CourseService {

    private static final String COURSE_SERVICE = "/courses";

    @WebMethod(exclude = true)
    @Override
    public void clear() {
        RequestBuilder request = new RequestBuilder("clear", "");
        try {
            post(COURSE_SERVICE, request, Void.class, null,
                    getItemNameForMethod("clear"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @WebMethod(exclude = true)
    @Override
    public int size() {
        RequestBuilder request = new RequestBuilder("size", "");
        try {
            Integer size = post(COURSE_SERVICE, request, Integer.class, null,
                    getItemNameForMethod("size"));
            return size;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @WebMethod(exclude = true)
    @Override
    public Course save(Course course) {
        RequestBuilder request = new RequestBuilder("save", course);
        try {
            Course saved = post(COURSE_SERVICE, request, Course.class, null,
                    getItemNameForMethod("save", Course.class));
            return saved;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @WebMethod(exclude = true)
    @Override
    public List<Course> enroll(List<Student> students) {
        RequestBuilder request = new RequestBuilder("enroll", students);
        try {
            return postWithListReturnType(request,
                    getItemNameForMethod("enroll", List.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Course> create(List<Course> courses) {
        RequestBuilder request = new RequestBuilder("create", courses);
        try {
            return postWithListReturnType(request,
                    getItemNameForMethod("create", List.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<Course> postWithListReturnType(RequestBuilder request,
            String item) throws NoSuchMethodException, Exception {
        Method m = CourseService.class.getMethod("count", List.class);
        Class<?> klass = m.getParameterTypes()[0].getClass();
        Type pKlass = m.getGenericParameterTypes()[0];
        return post(COURSE_SERVICE, request, klass, pKlass, item);
    }

    @WebMethod(exclude = true)
    @Override
    public List<Course> getCoursesForStudentId(Long studentId) {
        RequestBuilder request = new RequestBuilder("getCoursesForStudentId",
                studentId);
        try {
            return postWithListReturnType(request,
                    getItemNameForMethod("getCoursesForStudentId", Long.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @WebMethod(exclude = true)
    @Override
    public List<Course> query(Map<String, Object> criteria) {
        RequestBuilder request = new RequestBuilder("query", criteria);
        try {
            return postWithListReturnType(request,
                    getItemNameForMethod("query", Map.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @WebMethod(exclude = true)
    @Override
    public Course get(Long courseId) {
        RequestBuilder request = new RequestBuilder("get", courseId);
        try {
            Course resp = post(COURSE_SERVICE, request, Course.class, null,
                    getItemNameForMethod("get", Long.class));
            return resp;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @WebMethod(exclude = true)
    @Override
    public boolean exists(Course c, Student s) {
        RequestBuilder request = new RequestBuilder("exists", c);
        try {
            Boolean resp = post(COURSE_SERVICE, request, Boolean.class, null,
                    getItemNameForMethod("exists", Course.class, Student.class));
            return resp;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @WebMethod(exclude = true)
    @Override
    public int count(List<Course> c) {
        RequestBuilder request = new RequestBuilder("count", c);
        try {
            return post(COURSE_SERVICE, request, Integer.class, null,
                    getItemNameForMethod("count", List.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<Customer> postCustomers(RequestBuilder request)
            throws NoSuchMethodException, Exception {
        Method m = CourseService.class.getMethod("getCustomers", List.class);
        Class<?> klass = m.getParameterTypes()[0].getClass();
        Type pKlass = m.getGenericParameterTypes()[0];
        return post(COURSE_SERVICE, request, klass, pKlass,
                getItemNameForMethod("getCustomers", List.class));
    }

    @WebMethod(exclude = true)
    @Override
    public Collection<Customer> getCustomers(List<Customer> list) {
        RequestBuilder request = new RequestBuilder("getCustomers", list);
        try {
            return postCustomers(request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
