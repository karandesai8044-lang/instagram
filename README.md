# Instagram-style Login Clone (HTML/CSS + Java backend)

A visual clone of Instagram's login page, wired to a real working Java
backend so you can confirm every button/form actually fires.

## Files
- `index.html` — page structure (login box, Facebook login, sign up box, "Get the app", footer)
- `style.css` — Instagram-style look (colors, spacing, fonts)
- `script.js` — enables/disables the Log in button, show/hide password, sends the form to the backend
- `LoginServer.java` — tiny Java backend (uses only the built-in JDK, no frameworks/Maven needed)
- `login_attempts.txt` — created automatically the first time you submit the form

## How to run

You need a JDK installed (Java 11+). Check with:
```
java -version
javac -version
```

Then, inside this folder:
```
javac LoginServer.java
java LoginServer
```

You'll see:
```
Server running at http://localhost:8080
```

Open **http://localhost:8080** in your browser — that's the clone, served
directly by your Java backend (no separate web server needed).

## What happens when you click "Log in"

1. JS sends your typed username + password to `POST /login` on the Java server.
2. The server appends a line to `login_attempts.txt` (same folder) with a timestamp,
   so you can literally watch that file grow and confirm the button → backend
   wiring is working.
3. The page shows a green "Login attempt saved" banner back from the server.

There's no database and no real authentication check — every submit is
treated as a successful attempt, since the point right now is just proving
the request round-trip works end to end.

## Important note

This is a **local learning/demo project** — run it only on your own machine
for testing. Two things to keep in mind before this goes anywhere near
real users:
- It currently logs the password in plain text to `login_attempts.txt`,
  which is fine for you debugging on localhost, but never acceptable in a
  real product — a real login system must hash passwords and never log them.
- Don't deploy this publicly under anything that looks like the real
  Instagram — a working look-alike login form collecting real credentials
  is exactly what a phishing page looks like, even by accident.

When you're ready to turn this into a real auth system (hashed passwords,
a proper DB, session cookies), that's a good next step to build on top of
this skeleton.
