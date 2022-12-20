package edu.javacourse.studentorder.dao;

import edu.javacourse.studentorder.config.Config;
import edu.javacourse.studentorder.domain.*;
import edu.javacourse.studentorder.exception.DaoException;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StudentOrderDaoImpl implements StudentOrderDao {
    private static final String INSERT_ORDER = "INSERT INTO jc_student_order(" +
            "student_order_status, student_order_date, h_sur_name, h_given_name, " +
            "h_patronymic, h_date_of_birth, h_passport_seria, h_passport_number, h_passport_date, " +
            "h_passport_office_id, h_post_index, h_street_code, h_building, h_extension, h_apartment," +
            "h_university_id, h_student_number, " +
            "w_sur_name, w_given_name, w_patronymic, w_date_of_birth, w_passport_seria, w_passport_number, " +
            "w_passport_date, w_passport_office_id, w_post_index, w_street_code, w_building, w_extension, " +
            "w_apartment, w_university_id, w_student_number, certificate_id, register_office_id, marriage_date)" +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " +
            "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
    private static final String INSERT_CHILD =
            "INSERT INTO jc_student_child(" +
                    " student_order_id, c_sur_name, c_given_name, " +
                    " c_patronymic, c_date_of_birth, c_certificate_number, c_certificate_date, " +
                    " c_register_office_id, c_post_index, c_street_code, c_building, " +
                    " c_extension, c_apartment)" +
                    " VALUES (?, ?, ?, " +
                    " ?, ?, ?, ?, " +
                    " ?, ?, ?, ?, " +
                    " ?, ?)";
    private static final String SELECT_ORDERS = //соединяем данные с двух таблиц, данные ЗАГСА по id
            "SELECT so.*, ro.r_office_area_id, ro.r_office_name, " +
                    "po_h.p_office_area_id as h_p_office_area_id, po_h.p_office_name as h_p_office_name, " +
                    "po_w.p_office_area_id as w_p_office_area_id, po_w.p_office_name as w_p_office_name " +
                    "from jc_student_order so " +
                    "INNER JOIN jc_register_office ro ON ro.r_office_id = so.register_office_id " +
                    "INNER JOIN jc_passport_office po_h ON po_h.p_office_id = so.h_passport_office_id " +
                    "INNER JOIN jc_passport_office po_w ON po_w.p_office_id = so.w_passport_office_id " +
                    "WHERE student_order_status = ? order by student_order_date LIMIT ?";
    private static final String SELECT_CHILD = "" +
            "SELECT soc.*, ro.r_office_area_id, ro.r_office_name " +
            "FROM jc_student_child soc " +
            "INNER JOIN jc_register_office ro ON ro.r_office_id = soc.c_register_office_id " +
            "where student_order_id IN ";
    private static final String SELECT_ORDERS_FULL = //соединяем данные с двух таблиц, данные ЗАГСА по id
            "SELECT so.*, ro.r_office_area_id, ro.r_office_name, " +
                    "po_h.p_office_area_id as h_p_office_area_id, po_h.p_office_name as h_p_office_name, " +
                    "po_w.p_office_area_id as w_p_office_area_id, po_w.p_office_name as w_p_office_name, " +
                    "soc.*, ro_c.r_office_area_id, ro_c.r_office_name " +
                    "from jc_student_order so " +
                    "INNER JOIN jc_register_office ro ON ro.r_office_id = so.register_office_id " +
                    "INNER JOIN jc_passport_office po_h ON po_h.p_office_id = so.h_passport_office_id " +
                    "INNER JOIN jc_passport_office po_w ON po_w.p_office_id = so.w_passport_office_id " +
                    "INNER JOIN jc_student_child soc ON soc.student_order_id = so.student_order_id " +
                    "INNER JOIN jc_register_office ro_c ON ro_c.r_office_id = soc.c_register_office_id " +
                    "WHERE student_order_status = ? order by so.student_order_id LIMIT ?";


    //TODO REFACTORING - make one method
    private Connection getConnection() throws SQLException {

        Connection con = DriverManager.getConnection(
                Config.getProperty(Config.DB_URL),//сделали конфигурирование наших свойств подключения к БЗ
                Config.getProperty(Config.DB_LOGIN),
                Config.getProperty(Config.DB_PASSWORD)
        ); // подкючение к БД
        return con;
    }

    @Override
    public Long saveStudentOrder(StudentOrder so) throws DaoException {
        Long result = -1L;
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_ORDER, new String[]{"student_order_id"}))
            /*первое число, которое генерируется автоматичнски-  возвраащем его как массив из одного элемента*/ {

            //Чтобы запустить транзацию (она необходима, чтобы например, если ошибка возникает при запросе Insert_child,
            // то insert_order тоже не срабатывал. Без использования транзации, при ошибке в child, записать в order
            // все равно появится, поэтому выставляем параметр ниже
            con.setAutoCommit(false);//по умолчанию этот параметр является true, поэтому каждый
            //stmt. выполняется, так как теперь мы сами выставили этот параметр, то теперь мы сами управляем транзацией
            // с помощью двух команд
            //con.commit() - означает что все хорошо
            //con.rollback() - отменяет все команды, которые были сделаны в рамках этого Connection с момента когда
            //был объявлен false. Для этого воспользуемся блоком try

            try {
                // Header
                stmt.setInt(1, StudentOrderStatus.START.ordinal());
                stmt.setTimestamp(2, java.sql.Timestamp.valueOf(LocalDateTime.now()));


                setParamsForAdult(stmt, 3, so.getHusband());
                setParamsForAdult(stmt, 18, so.getWife());

                // Marriage
                stmt.setString(33, so.getMarriageCertificateId());
                stmt.setLong(34, so.getMarriageOffice().getOfficeId());
                stmt.setDate(35, java.sql.Date.valueOf(so.getMarriageDate()));

                stmt.executeUpdate();//оюновление записей, возвращает количество измененный записей

                ResultSet gkRs = stmt.getGeneratedKeys();//возвраащет список Set тех полей, которые вы сгенерировали
                if (gkRs.next()) {
                    result = gkRs.getLong(1); //вытаскиваем значение первой колонки
                }
                gkRs.close();

                saveChildren(con, so, result);
                con.commit();
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            throw new DaoException(ex);
        }
        return result;
    }

    private void saveChildren(Connection con, StudentOrder so, Long soId) throws SQLException {
        try (PreparedStatement stmt = con.prepareStatement(INSERT_CHILD)) {

            /*//если детей допустим очень много, то можно сделать счетчик:
            int counter = 0;//и копим его*/

            for (Child child : so.getChildren()) {
                stmt.setLong(1, soId);
                setParamsForChild(stmt, child);
                // stmt.executeUpdate();// (возвращает количество записей, затронутых данным изменением)
                // его можно заменить на:
                stmt.addBatch();//то есть добавление команды в пакет(пакетное исполнение) для улучшения производительност
                // так называемая буферизация, накапливая несколько маленьких действий,
                // делаем их потом разом, а не по одному

                /* counter++; // копим счетчик*/

                // if (counter>10000) { //чтобы пакет не был слишком большим*/
                // stmt.executeBatch();
                // counter=0;}
            }
            // if (counter>0) {
            stmt.executeBatch();//берет весь пакет и исполняет его, возвращает массив целых чисел
            // }
        }
    }

    private void setParamsForChild(PreparedStatement stmt, Child child) throws SQLException {
        setParamsForPerson(stmt, 2, child);
        stmt.setString(6, child.getCertificateNumber());
        stmt.setDate(7, java.sql.Date.valueOf(child.getIssueDate()));
        stmt.setLong(8, child.getIssueDepartment().getOfficeId());
        setParamsForAddress(stmt, 9, child);
    }

    private void setParamsForAdult(PreparedStatement stmt, int start, Adult adult) throws SQLException {
        setParamsForPerson(stmt, start, adult);
        stmt.setString(start + 4, adult.getPassportSeria());
        stmt.setString(start + 5, adult.getPassportNumber());
        stmt.setDate(start + 6, Date.valueOf(adult.getIssueDate()));
        stmt.setLong(start + 7, adult.getIssueDepartment().getOfficeId());
        setParamsForAddress(stmt, start + 8, adult);
        stmt.setLong(start + 13, adult.getUniversity().getUniversityId());
        stmt.setString(start + 14, adult.getStudentId());
    }

    private void setParamsForPerson(PreparedStatement stmt, int start, Person person) throws SQLException {
        stmt.setString(start, person.getSurName());
        stmt.setString(start + 1, person.getGivenName());
        stmt.setString(start + 2, person.getPatronymic());
        stmt.setDate(start + 3, Date.valueOf(person.getDateOfBirth()));
    }

    private void setParamsForAddress(PreparedStatement stmt, int start, Person person) throws SQLException {
        Address h_address = person.getAddress();
        stmt.setString(start, h_address.getPostCode());
        stmt.setLong(start + 1, h_address.getStreet().getStreetCode());
        stmt.setString(start + 2, h_address.getBuilding());
        stmt.setString(start + 3, h_address.getExtension());
        stmt.setString(start + 4, h_address.getApartment());
    }

    @Override
    public List<StudentOrder> getStudentOrders() throws DaoException {
        return getStudentOrdersOneSelect();
        //return getStudentOrdersTwoSelect();
    }

    private List<StudentOrder> getStudentOrdersOneSelect() throws DaoException {
        List<StudentOrder> result = new LinkedList<>();
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_ORDERS_FULL)) {

            Map<Long, StudentOrder> maps = new HashMap<>(); // для того, чтобы заявление не дублировалось если двое детей и более

            stmt.setInt(1, StudentOrderStatus.START.ordinal());
            //первому параметру = порядок в ENUM, для START ЭТО 0, В ЗАПРОСЕ СТОИТ ЗНАК ?
            int limit = Integer.parseInt(Config.getProperty(Config.DB_LIMIT));
            stmt.setInt(2, limit);

            /*//один вариант с листом
            //формируем строку для SELECT_CHILD
            List <Long> ids = new LinkedList<>();*/
            ResultSet rs = stmt.executeQuery();
            int counter = 0;//счетчик для записей
            while (rs.next()) {
                Long soId = rs.getLong("student_order_id");
                if (!maps.containsKey(soId)) { // проверяем есть ли уже заявка, если нету, то создаем
                    StudentOrder so = getFullStudentOrder(rs);

                    result.add(so);
                    maps.put(soId, so);
                }
                StudentOrder so = maps.get(soId); //вытаскиваем созданную заявку из мэпа
                so.addChild(fillChild(rs)); //добавляем ребенка в заявку
                counter++;
                //  ids.add(so.getStudentOrderId());//добавляем номер заявление, чтобы сформировать его список
            }
            if (counter >= limit) {//если последняя семья не полностью влезла в 1000 записей, удаляем ее
                result.remove(result.size() - 1);
            }
            //findChildren(con, result);

           /* StringBuilder sb = new StringBuilder("("); // для формирования строки из номеров заявления 1, 2, 3...
            for (Long id: ids){
                sb.append((sb.length()>1 ? ",": "")+String.valueOf(id));
            }
            sb.append(")");
            System.out.println(sb.toString());*/
            rs.close();

        } catch (SQLException ex) {
            throw new DaoException(ex);
        }
        return result;
    }


    private List<StudentOrder> getStudentOrdersTwoSelect() throws DaoException {
        List<StudentOrder> result = new LinkedList<>();
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_ORDERS)) {

            stmt.setInt(1, StudentOrderStatus.START.ordinal());
            //первому параметру = порядок в ENUM, для START ЭТО 0, В ЗАПРОСЕ СТОИТ ЗНАК ?
            stmt.setInt(2, Integer.parseInt(Config.getProperty(Config.DB_LIMIT)));

            /*//один вариант с листом
            //формируем строку для SELECT_CHILD
            List <Long> ids = new LinkedList<>();*/
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                StudentOrder so = getFullStudentOrder(rs);

                result.add(so);
                //  ids.add(so.getStudentOrderId());//добавляем номер заявление, чтобы сформировать его список
            }
            findChildren(con, result);

           /* StringBuilder sb = new StringBuilder("("); // для формирования строки из номеров заявления 1, 2, 3...
            for (Long id: ids){
                sb.append((sb.length()>1 ? ",": "")+String.valueOf(id));
            }
            sb.append(")");
            System.out.println(sb.toString());*/
            rs.close();

        } catch (SQLException ex) {
            throw new DaoException(ex);
        }
        return result;
    }

    private StudentOrder getFullStudentOrder(ResultSet rs) throws SQLException {
        StudentOrder so = new StudentOrder();
        fillStudentOrder(rs, so); //заполняем студенческую заявку, то есть вытаскиваем данные по запросу SELECT
        fillMarriage(rs, so);

        so.setHusband(fillAdult(rs, "h_"));
        so.setWife(fillAdult(rs, "w_"));
        return so;
    }

    private void fillStudentOrder(ResultSet rs, StudentOrder so) throws SQLException {
        so.setStudentOrderId(rs.getLong("student_order_id"));
        so.setStudentOrderDate(rs.getTimestamp("student_order_date").toLocalDateTime());
        so.setStudentOrderStatus(StudentOrderStatus.fromValue(rs.getInt("student_order_status")));

    }

    private void fillMarriage(ResultSet rs, StudentOrder so) throws SQLException {
        /*certificate_id varchar(20) not null,
                register_office_id integer not null,
                marriage_date date not null,*/
        so.setMarriageCertificateId(rs.getString("certificate_id"));
        so.setMarriageDate(rs.getDate("marriage_date").toLocalDate());
        Long roId = rs.getLong("register_office_id");
        String areaId = rs.getString("r_office_area_id");
        String name = rs.getString("r_office_name");

        RegisterOffice ro = new RegisterOffice(roId, areaId, name);
        so.setMarriageOffice(ro);
    }

    private Adult fillAdult(ResultSet rs, String pref) throws SQLException {
        Adult adult = new Adult();
        adult.setSurName(rs.getString(pref + "sur_name"));
        adult.setGivenName(rs.getString(pref + "given_name"));
        adult.setPatronymic(rs.getString(pref + "patronymic"));
        adult.setDateOfBirth(rs.getDate(pref + "date_of_birth").toLocalDate());
        adult.setPassportSeria(rs.getString(pref + "passport_seria"));
        adult.setPassportNumber(rs.getString(pref + "passport_number"));
        adult.setIssueDate(rs.getDate(pref + "passport_date").toLocalDate());

        Long poId = rs.getLong(pref + "passport_office_id");
        String poArea = rs.getString(pref + "p_office_area_id");
        String poName = rs.getString(pref + "p_office_name");
        PassportOffice po = new PassportOffice(poId, poArea, poName);
        adult.setIssueDepartment(po);
        Address adr = new Address();
        Street st = new Street(rs.getLong(pref + "street_code"), "");
        adr.setPostCode(rs.getString(pref + "post_index"));
        adr.setStreet(st);
        adr.setBuilding(rs.getString(pref + "building"));
        adr.setExtension(rs.getString(pref + "extension"));
        adr.setApartment(rs.getString(pref + "apartment"));
        adult.setAddress(adr);
        University uni = new University(rs.getLong(pref + "university_id"), "");
        adult.setUniversity(uni);
        adult.setStudentId(rs.getString(pref + "student_number"));
        return adult;
    }

    private void findChildren(Connection con, List<StudentOrder> result) throws SQLException {
        String cl = "(" + result.stream().map(so -> String.valueOf(so.getStudentOrderId()))
                .collect(Collectors.joining(",")) + ")"; //формируем строку для SELECT_CHILD

        Map<Long, StudentOrder> maps = result.stream().collect(Collectors
                .toMap(so -> so.getStudentOrderId(), so -> so)); //создаем ьэп из ID и заявления для быстрого поиска нужного заявление

        try (PreparedStatement stmt = con.prepareStatement(SELECT_CHILD + cl)) { /*создаем запрос, в который передаем параметр*/
            ResultSet rs = stmt.executeQuery();//возвраащет список Set тех полей, которые вы сгенерировали
            //выполняем запрос
            //  ResultSet - это множство записей, по которому можно двигаться с помощью метода next
            while (rs.next()) {
                Child ch = fillChild(rs);
                StudentOrder so = maps.get(rs.getLong("student_order_id")); //по ID находим студ заявление
                so.addChild(ch);
            }
        }
    }

    private Child fillChild(ResultSet rs) throws SQLException {
        String surName = rs.getString("c_sur_name");
        String givenName = rs.getString("c_given_name");
        String patronymic = rs.getString("c_patronymic");
        LocalDate dateOfBirth = rs.getDate("c_date_of_birth").toLocalDate();
        Child child = new Child(surName, givenName, patronymic, dateOfBirth);
        child.setCertificateNumber(rs.getString("c_certificate_number"));
        child.setIssueDate(rs.getDate("c_certificate_date").toLocalDate());
        Long roId = rs.getLong("c_register_office_id");
        String roArea = rs.getString("r_office_area_id");
        String roName = rs.getString("r_office_name");
        RegisterOffice ro = new RegisterOffice(roId, roArea, roName);
        child.setIssueDepartment(ro);
        Address adr = new Address();
        Street st = new Street(rs.getLong("c_street_code"), "");
        adr.setPostCode(rs.getString("c_post_index"));
        adr.setStreet(st);
        adr.setBuilding(rs.getString("c_building"));
        adr.setExtension(rs.getString("c_extension"));
        adr.setApartment(rs.getString("c_apartment"));
        child.setAddress(adr);

        return child;
    }
}
