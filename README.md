Digest-аутентификация на примере HTTP-сервера  
Для работы с проектом требуется jdk-11. Установить и настроить ее можно следующими командами:  
_curl https://download.java.net/openjdk/jdk11/ri/openjdk-11+28_linux-x64_bin.tar.gz | tar -xz_  
_export PATH=/*dir*/jdk-11:$PATH_  
_export JAVA_HOME=/*dir*/jdk-11_  

Для запуска сервера требуется перейти в директорию проекта и выполнить следующие команды:
1. Развертывание БД:  
   _docker-compose up_  
В случае возникновения ошибки "mkdir: cannot create directory '/bitnami/cassandra': Permission denied" выполните следующую команду:  
   _sudo chown -R 1001:1001 ~/apps/cassandra:/bitnami_  
2. Сборка проекта:  
   _mvn compile_  
3. Запуск:  
   _mvn exec:java -Dexec.mainClass="main.Main"_    
Также можно запустить, передав параметры "add _username example.com password_", где username, password - данные 
пользователя для добавления в БД. Пользователь, параметры еоторого были переданы, будет добавлен в БД (для добавления нескольких пользователей требуется запустить сервер нескольео раз, передавая за раз параметры одного пользователя):  
   _mvn exec:java -Dexec.mainClass="main.Main" -Dexec.args="add username example.com password"_  

Сервис работает на порту 8094. После введения корректных данных пользователя доступны следующие ресурсы:  
- "/page.html" (GET, PUT - при вводе сообщения в поле и нажатии кнопки)  
- "/" (GET)
