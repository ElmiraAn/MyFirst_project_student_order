package edu.javacourse.studentorder.dao;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class SimpleRunner {
    public static void main(String[] args) {
        SimpleRunner sr = new SimpleRunner();

        sr.runTest();

    }

    private void runTest() {
        //получаем описание нужного класса
        try {
            Class<?> cl = Class.forName("edu.javacourse.studentorder.dao.DictionaryDaoImplTest");
            //получить списко конструкторов этого класса
            Constructor cst = cl.getConstructor();
            Object entity = cst.newInstance();//с помощью конструктора создаем сущность экземпляр объекта
            Method[] methods = cl.getMethods(); //получаем массив методов
            for (Method m:methods){
                Test ann = m.getAnnotation(Test.class);//проверяем имеет ли метод аннотацию
                if (ann!=null) {
                    m.invoke(entity);//передаем объект вызываемомму методу
                }
            }

        }catch (Exception ex){
            ex.printStackTrace();
        }


    }
}
