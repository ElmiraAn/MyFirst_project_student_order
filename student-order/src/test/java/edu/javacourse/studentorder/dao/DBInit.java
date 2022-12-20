package edu.javacourse.studentorder.dao;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Collectors;

public class DBInit {
    public static void StartUp() throws Exception {
        //получаем ссылку на файл, который находится в нашем же проекте
        URL url1 = DictionaryDaoImplTest.class.getClassLoader()
                .getResource("student_project.sql");//получаем URL файла
        URL url2 = DictionaryDaoImplTest.class.getClassLoader()
                .getResource("student_data.sql");//получаем URL файла
        List<String> str1 = Files.readAllLines(Paths.get(url1.toURI()));
        //прочитаем все строки файл с указанием пути path
        //делаем их них одну строку
        List<String> str2 = Files.readAllLines(Paths.get(url2.toURI()));
        String sql1 = str1.stream().collect(Collectors.joining());
        String sql2 = str2.stream().collect(Collectors.joining());
        //исполняем строку, создаем БД
        try (Connection con = ConnectionBuilder.getConnection();
             Statement stmt = con.createStatement();) {
            stmt.executeUpdate(sql1);//выполняем запрос
            stmt.executeUpdate(sql2);//выполняем запрос

        }
    }
}
