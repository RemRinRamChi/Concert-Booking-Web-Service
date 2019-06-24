# Concert Booking Web Service

Scalable JAX-RS REST web service for a small venue with 374 seats. 
- Seats are classified into 3 price bands (A,B,c).
- Only authenticated clients can enquire and make seat reservations.
- Seats are locked for a short time when reserved, and released when no confitmation is given after a while.
- Clients can subscribe to event news stories.

Java Persistence API is used for persistence of objects in a relational database.
