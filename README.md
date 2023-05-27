Digest-аутентификация на примере HTTP-сервера
Для запуска сервера требуется перейти в директорию проекта и выполнить следующие команды:
1. Развертывание БД:  
   _docker-compose up_
2. Сборка проекта:  
   _mvn compile_
3. Запуск:  
   _mvn exec:java -Dexec.mainClass="main.Main"_  
Также можно запустить, передав параметры "add _username realm password_", где username, realm, password - данные 
пользователя для добавления в БД:  
   _mvn exec:java -Dexec.mainClass="main.Main" -Dexec.args="add username realm password"_
