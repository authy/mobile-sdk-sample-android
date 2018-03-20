# TwilioAuthenticator SDK - Android Sample app

Welcome to the Twilio Authenticator Android SDK Sample application. This application demonstrates how to use the mobile SDK inside an Android app.

## How to Run

* **Step 1:** Clone the repository to your local machine

* **Step 2:** Open Android Studio and import the project by selecting the build.gradle file from the cloned repository

* **Step 3:** Install the Authenticator SDK dev preview to your maven local

```
cd authenticator-2.0.0-preview/

mvn install:install-file \
   -DgroupId=com.twilio \
   -DartifactId=authenticator \
   -Dversion=2.0.0-preview \
   -Dpackaging=aar \
   -Dfile=authenticator-2.0.0-preview.aar \
   -Dsources=authenticator-2.0.0-preview-sources.jar \
   -Djavadoc=authenticator-2.0.0-preview-javadoc.jar \
   -DpomFile=authenticator-2.0.0-preview.pom
```

If you need maven client installed you can easily do it by running
```
brew install maven
```

* **Step 4:** [A backend application to handle the device registration](https://github.com/authy/authenticator-sdk-backend)

* **Step 5:** Setup your firebase account, downloading the `google-services.json` configuration file following [this link](https://firebase.google.com/docs/android/setup)

### Learn more
- Check out the full documentation at https://www.twilio.com/docs/quickstart/twilioauth-sdk-quickstart-tutorials
- Contact the Twilio support team at help@twilio.com
