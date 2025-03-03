1. Установите необходимые инструменты
   Перед запуском убедитесь, что у вас установлены:
   ✅ Java 17+ (проверьте: java -version)
   ✅ Gradle (проверьте: ./gradlew -v)
   ✅ PostgreSQL (если используется)
2. Настройте базу данных
Создайте базу данных в PostgreSQL:
   CREATE DATABASE my_database;
   Проверьте application.yml или application.properties и укажите корректные параметры:
   spring.datasource.url=jdbc:postgresql://localhost:5432/patents_db
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   spring.jpa.hibernate.ddl-auto=update
   spring.jpa.show-sql=true

3. Запустите приложение
    Запустите приложение с помощью Gradle:
    ./gradlew bootRun
    Приложение будет доступно по адресу: http://localhost:8080

4. Запуск парсеров
    Парсинг данных
   🔹 Запуск всех парсеров
    POST /api/patents/parse
   ✅ Запускает парсинг всех доступных категорий всех парсеров.

   🔹 Запуск парсера по имени
    POST /api/patents/parse/{parserName}
    ✅ Запускает все категории указанного парсера.

    🔹 Запуск парсера по имени и категории
    POST /api/patents/parse/{parserName}/{category}
    ✅ Запускает указанную категорию указанного парсера.

   🔹 Запуск парсера госреестра для всех категорий с конца и начала списка
    POST /api/patents/parse/gosreestr/both
    ✅ Запускает парсинг госреестра для всех категорий с конца и начала списка.

   🔹 Запуск парсера госреестра для указанной категорий с конца и начала списка
    POST /api/patents/parse/gosreestr/{category}/both
    ✅ Запускает парсинг госреестра для указанной категории с конца и начала списка.

    🔹 Парсинг с конца списка для указанной категории
   POST /api/patents/parse/{parserName}/{category}/end
   ✅ Начинает парсинг с последней записи в категории.

    🔹 Парсинг с начала списка для указанной категории
    POST /api/patents/parse/{parserName}/{category}/start
    ✅ Начинает парсинг с первой записи в категории.

    {parserName} - имя парсера (gosreestr, ebulletin)
    {category} - категория парсера на русском(Селекционные достижения, Товарные знаки, Изобретения, Полезные модели, Общеизвестные товарные знаки)

5.  Поиск и фильтрация патентов (UI)
    http://localhost:8080/patents
    ✅ Показывает список всех патентов с фильтрацией по параметрам:

    query – поиск по названию
    startDate, endDate – фильтр по дате
    siteType – источник
    expired – только истекшие
    category – категория

    