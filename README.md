# Covid19 Data Refresh Automation
cvirus download the data source from github, massage the data to provide additional data dimension. Once ready, it will refresh the google sheet with new batch of data.

## How It Works
This application is designed to be a serverless app deployed in AWS Lambda. It will be triggered from a cron job rule defined in AWS CloudWatch Event. Application logs can be found in CloudWatch Logs > Log Groups

Data source downloaded from GitHub is in CSV format. Once processed, it will be stored into Google Sheet. Existing data (except the header row) in Google Sheet will always be cleared before inserting the new batch.

## Installation
Build with Maven and deployed to AWS Lambda

### Regular JVM Runtime
```bash
mvn clean package

sh target/manage.sh [create|update|delete|invoke]
```

### Native Image
```bash
mvn clean install -Pnative -Dnative-image.docker-build=true

sh target/manage.sh native [create|update|delete|invoke]
```

## Hints & Tips
### Interacting With Google Sheet In Java Code
#### Create a Google Sheet

- Take note of the sheet ID value found in the sheet URL 
```
https://docs.google.com/spreadsheets/d/{ID-value}/edit
```

- The ID will be configured in the app properties file

#### Create a GCP project > Service Account. 
- Go into the Service Account, add a key (json) & download the key.
- The downloaded key file in json will be placed under resources directory in the project
- The note of the email address found in the Service Account.
- Go into the Google Sheet, click on **Share** on top right corner. 
- Add the email address to share with and set the permission to Editor.

## Technologies
- GraalVM (Java 11)
- Quarkus IO
- Maven
- Google Sheets
- AWS Lambda
- AWS CloudWatch (Event)

## References
- https://github.com/datasets/covid-19
- https://quarkus.io/guides/amazon-lambda
- https://quarkus.io/guides/writing-native-applications-tips
- https://developers.google.com/sheets/api/quickstart/java
- https://cloud.google.com/iam/docs/creating-managing-service-accounts
- https://developers.google.com/identity/protocols/oauth2/service-account
- https://docs.aws.amazon.com/AmazonCloudWatch/latest/events/ScheduledEvents.html