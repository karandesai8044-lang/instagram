# Enquiry Form Demo (HTML/CSS + Java backend)

This project is a local demo website where the old login-style page is repurposed as an enquiry form. Users fill in their details, client-side validation runs, and the form submits to a Java backend.

## Files
- `index.html` — enquiry form UI
- `style.css` — styling, spacing, validation error states
- `script.js` — field validation, button state, submission logic
- `LoginServer.java` — Java backend that validates input and attempts to send email
- `login_attempts.txt` — fallback log for form submissions
- `enquiries.txt` — local backup when SMTP is not configured

## Run it locally

You need a JDK installed (Java 17+ is fine). Check with:
```
java -version
javac -version
```

Then, in this folder:
```
C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot\bin\javac.exe LoginServer.java
C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot\bin\java.exe LoginServer
```

Open http://localhost:8080 in the browser.

## Email configuration

To deliver the enquiry directly to your email address, configure environment variables before starting the server:

PowerShell:
```powershell
$env:SMTP_HOST = "smtp.gmail.com"
$env:SMTP_PORT = "587"
$env:SMTP_USERNAME = "karandesai8044@gmail.com"
$env:SMTP_PASSWORD = "your-16-character-gmail-app-password"
$env:EMAIL_TO = "karandesai8044@gmail.com"
```

Then start the app again.

Important: Gmail requires an App Password, not your normal account password.

## What happens on submit

1. The browser validates all required fields.
2. If validation fails, clear inline error messages appear under the relevant fields.
3. On success, the app posts the enquiry JSON to the Java server.
4. The backend validates the payload again.
5. It attempts to email the enquiry with subject:
   `New enquiry from website`
6. If SMTP is not configured, the data is saved locally to `enquiries.txt` as a backup.

## Notes

This is a local learning/demo project. It is not a production-ready login or email system and should not be used for real customer data without proper security, secrets management, and proper mail delivery configuration.
