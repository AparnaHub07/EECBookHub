# EEC BookHub — Full Stack

A student-to-student book sharing web application for Easwari Engineering College.

## Stack
- Frontend: HTML, CSS, Bootstrap, JavaScript
- Backend: Java 21 + Spring Boot
- Database: PostgreSQL + Spring Data JPA/Hibernate
- Passwords: BCrypt
- Forgot password: real email OTP through SMTP
- Penalty payment: direct borrower-to-provider UPI with provider confirmation

## Main features
- College-email registration and login
- Real forgot-password OTP sent to the registered college email
- Provider phone number in Registration/Profile and shown with pickup/return details
- Free-text pickup and return locations
- Provider-controlled book availability: Available / Not Available
- Borrow requests, due dates, returns, notifications and reviews
- Late penalty: ₹40 for day 1 + ₹10 for every additional overdue day
- UPI penalty flow: borrower pays provider directly, submits UPI transaction reference, provider verifies receipt and confirms payment

## Run locally
## How to Run the Project

1. Install PostgreSQL and create the database `eecbookhub` using `database/create-database.sql`.
2. Configure your PostgreSQL credentials in the application settings.
3. For OTP email functionality, configure the SMTP settings using `.env.example`.
4. Start the application using Maven:

   
   mvn spring-boot:run


## Production notes
Use HTTPS in deployment and set `COOKIE_SECURE=true`. Never commit SMTP passwords or database passwords. For direct UPI, EEC BookHub does not hold student funds; the provider must verify the incoming payment before confirming it in the app.
