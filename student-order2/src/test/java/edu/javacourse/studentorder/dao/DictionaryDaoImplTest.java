package edu.javacourse.studentorder.dao;

import edu.javacourse.studentorder.domain.CountryArea;
import edu.javacourse.studentorder.domain.PassportOffice;
import edu.javacourse.studentorder.domain.RegisterOffice;
import edu.javacourse.studentorder.domain.Street;
import edu.javacourse.studentorder.exception.DaoException;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;


public class DictionaryDaoImplTest {
    private static final Logger logger = LoggerFactory.getLogger(DictionaryDaoImplTest.class);//делаем логирование

    @BeforeClass //выполняется при запуске всех тестов, выполнится только один раз, используем для создания БД
    public static void startUp() throws Exception {
        DBInit.StartUp();
    }

    // @Before  //выполняется перед каждым тестом
    //@AfterClass //запускается после всех тестов
    // @After // после каждого теста


    @Test
    public void testStreet() throws DaoException {
        LocalDateTime dt1 = LocalDateTime.now();
        LocalDateTime dt2 = LocalDateTime.now();

        logger.info("TEST {} {}", dt1, dt2);
        List<Street> d = new DictionaryDaoImpl().findStreets("про");//ищем ноабор улиц с "про"
        //проверяем количесство улиц
        Assert.assertTrue(d.size() == 2);
    }

    @Test
    public void testPassportOffice() throws DaoException {
        List<PassportOffice> po = new DictionaryDaoImpl().findPassportOffices("010020000000");
        Assert.assertTrue(po.size() == 2);
    }

    @Test
    public void testRegisterOffice() throws DaoException {
        List<RegisterOffice> ro = new DictionaryDaoImpl().findRegisterOffices("010010000000");
        Assert.assertTrue(ro.size() == 2);
    }

    @Test
    public void testArea() throws DaoException {
        List<CountryArea> ca1 = new DictionaryDaoImpl().findAreas("");
        Assert.assertTrue(ca1.size() == 2);
        List<CountryArea> ca2 = new DictionaryDaoImpl().findAreas("020000000000");
        Assert.assertTrue(ca2.size() == 2);
        List<CountryArea> ca3 = new DictionaryDaoImpl().findAreas("020010000000");
        Assert.assertTrue(ca3.size() == 2);
        List<CountryArea> ca4 = new DictionaryDaoImpl().findAreas("020010010000");
        Assert.assertTrue(ca4.size() == 2);
    }
}