# EEC BookHub REST API

## Authentication
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/auth/me`
- `POST /api/auth/forgot-password/request`
- `POST /api/auth/forgot-password/reset`

## Users and books
- `GET /api/users`
- `PATCH /api/users/me` — update phone and/or UPI ID
- `GET /api/books`
- `GET /api/books/{id}`
- `POST /api/books`
- `PATCH /api/books/{id}/status` — provider changes Available / Not Available
- `DELETE /api/books/{id}`
- `GET /api/books/{id}/contact`
- `GET /api/books/borrowing/{id}/contact`

## Borrowing
- `GET /api/borrow-requests`
- `POST /api/borrow-requests`
- `POST /api/borrow-requests/{id}/respond`
- `GET /api/borrowings`
- `POST /api/borrowings/{id}/return`

## Direct UPI penalty payment
- `GET /api/payments/upi/{borrowingId}` — provider UPI details and payment status
- `POST /api/payments/upi/report` — borrower submits UPI transaction/reference number
- `POST /api/payments/upi/confirm` — provider confirms or rejects the reported payment

## Notifications and reviews
- `GET /api/notifications`
- `POST /api/notifications/{id}/read`
- `POST /api/notifications/read-all`
- `GET /api/reviews`
- `POST /api/reviews`

## Health
- `GET /api/health`
