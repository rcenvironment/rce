The SQL components are configured to work with the following parameters:

host: localhost
port: 3306
database: rce_test
user: root
password: scroot

Please adapt the database configuration accordingly, or change the parameters in the configuration files.

To create a simple test database, run the following statements:

1. Create scheme:
CREATE DATABASE `rce_test2` /*!40100 DEFAULT CHARACTER SET utf8 */;

2. Create table:
CREATE TABLE `test_data` (
  `idtest_data` int(11) NOT NULL,
  `name` varchar(45) DEFAULT NULL,
  `number` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`idtest_data`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

3. Populate table:
INSERT INTO `rce_test`.`test_data` (`idtest_data`, `name`, `number`) VALUES ('1', 'Anton', '11');
INSERT INTO `rce_test`.`test_data` (`idtest_data`, `name`, `number`) VALUES ('2', 'Berta', '23');
INSERT INTO `rce_test`.`test_data` (`idtest_data`, `name`, `number`) VALUES ('3', 'Charly', '35');
INSERT INTO `rce_test`.`test_data` (`idtest_data`, `name`, `number`) VALUES ('4', 'Donna', '42');
INSERT INTO `rce_test`.`test_data` (`idtest_data`, `name`, `number`) VALUES ('5', 'Emil', '51');
