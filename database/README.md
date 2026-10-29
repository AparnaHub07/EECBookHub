# PostgreSQL database

EEC BookHub now uses PostgreSQL instead of local H2 `.mv.db` / `.lock.db` files.

The database structure is represented by the JPA entity classes under:
`src/main/java/com/eec/bookhub/model/`

Main tables: `users`, `books`, `borrow_requests`, `borrowings`, `notifications`, `reviews`, `password_reset_otps`, and `payments`.

Spring Data JPA/Hibernate creates/updates the tables using the PostgreSQL connection configured in `application.properties`.
