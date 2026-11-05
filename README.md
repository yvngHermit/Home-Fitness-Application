# Home Fitness

<p align="center">
  <img src="\drawable\home_fitness_logo.png" alt="Home Fitness logo" width="180" />
</p>

<p align="center"><strong>A personal fitness companion for workouts, nutrition and progress tracking.</strong></p>

## Overview

Home Fitness is an Android application built with Kotlin and XML layouts. It brings exercise discovery, workout sessions, meal planning and personal fitness tracking into one mobile experience. The app is designed to make everyday fitness tasks straightforward: users can find exercises, create and complete workouts, search for foods, record meals and review their activity.

The application uses external services for exercise and food information, and Firebase services for account identity and cloud-saved user data.

## Main goals

- Make workout discovery and workout creation accessible from a single app.
- Support both exercise-based and timed workout activities where the workout flow provides them.
- Help users plan meals and review calorie information.
- Keep account-related profile and activity information associated with the signed-in user.
- Present progress and fitness information in a clear, mobile-friendly interface.

## Features

### Account and profile
- Email/password registration and sign-in through Firebase Authentication.
- Google sign-in through Android Credential Manager and Firebase Authentication.
- Password-reset email flow.
- User profile data stored under the signed-in user's Firestore document.

### Exercise discovery and workouts
- Exercise catalogue supplied by the wger API.
- Exercise categories including Upper Body, Lower Body, Core, Cardio and Full Body.
- Exercise details and exercise selection for custom workouts.
- Custom workout creation and storage in the user's Firestore data.
- Workout session screen and workout completion logging.
- Cached exercise data to support previously loaded exercises when network access is unavailable.

### Nutrition
- Food search using the USDA FoodData Central API.
- Food descriptions and calorie values displayed from returned nutrient data.
- Meal diary entries stored under the user's Firestore account.
- Meal categories such as breakfast, lunch, dinner and snacks, as implemented in the nutrition screen.

### Progress and settings
- Workout history and weekly activity summary.
- Current streak and latest recorded weight where available.
- Profile and settings screens for personal preferences.
- Local Android notifications and Firebase Cloud Messaging support for app notifications.

## Design considerations

- **Mobile-first navigation:** Activities are separated by purpose and connected through the app's navigation controls.
- **Readable information hierarchy:** Workout names, food results, calorie values and progress metrics are presented as distinct content elements.
- **Consistent visual identity:** The interface uses a fitness-oriented green/lime palette and the Home Fitness logo.
- **Feedback and usability:** Loading, empty and error states help communicate when remote data is being retrieved or is unavailable.
- **Network resilience:** Exercise caching allows the app to reuse previously downloaded exercise records.
- **Privacy-conscious configuration:** The USDA API key is read from developer-local Gradle configuration rather than typed into the app interface. Firebase configuration is supplied through the project's Google services configuration file; do not publish private credentials or unrestricted secrets in a public repository.

## API routing and data flow

The app integrates two external REST APIs. They serve different parts of the application and use separate Retrofit interfaces and clients.

### 1. wger — exercise data

- **Base URL:** `https://wger.de/`
- **Interface:** `app/src/main/java/com/example/homefitness/api/WgerApiService.kt`
- **Retrofit client:** `app/src/main/java/com/example/homefitness/api/RetrofitInstance.kt`
- **Main endpoint:** `GET /api/v2/exerciseinfo/`
- **Detail endpoint:** `GET /api/v2/exerciseinfo/{id}/`
- **Response model:** `app/src/main/java/com/example/homefitness/models/WgerResponse.kt`
- **Used by:** exercise discovery, workout selection and homepage workout recommendations.

Routing sequence:

1. An Activity calls `RetrofitInstance.wgerApi`.
2. Retrofit maps the Kotlin function in `WgerApiService` to the endpoint and query parameters.
3. OkHttp sends the HTTPS request to `wger.de`.
4. Gson converts the JSON response into `WgerExerciseResponse` / `WgerExercise` model objects.
5. The Activity filters and displays the exercise records, and the exercise cache can store retrieved data.

Example request shape (illustrative):

```http
GET https://wger.de/api/v2/exerciseinfo/?language=2&limit=100&offset=0
```

### 2. USDA FoodData Central — food and nutrient data

- **Base URL:** `https://api.nal.usda.gov/`
- **Interface:** `app/src/main/java/com/example/homefitness/api/FoodDataApi.kt`
- **Retrofit client:** `app/src/main/java/com/example/homefitness/api/FoodDataClient.kt`
- **Endpoint:** `GET /fdc/v1/foods/search`
- **Response models:** `app/src/main/java/com/example/homefitness/models/FoodModels.kt`
- **Used by:** food search in the Nutrition screen.

Routing sequence:

1. The user enters a food search term in the Nutrition screen.
2. `NutritionActivity` reads `BuildConfig.USDA_API_KEY` and calls `FoodDataClient.api.searchFoods(...)`.
3. Retrofit sends the query, API key and page size to FoodData Central over HTTPS.
4. Gson parses the response into `FoodSearchResponse` and `FoodItem` objects.
5. The screen displays the food description and derives calories from nutrient ID `1008` when that nutrient is present.
6. When a meal is recorded, the app writes the meal entry to the signed-in user's Firestore `mealLogs` subcollection.

Example request shape (do not publish a real key):

```http
GET https://api.nal.usda.gov/fdc/v1/foods/search?query=banana&api_key=YOUR_API_KEY&pageSize=20
```

### Firebase routing (backend services)

Firebase is not one of the two external food/exercise catalogue APIs. It provides the app's account and cloud-data services:

- **Firebase Authentication:** sign-in, registration, Google credential exchange and password reset.
- **Cloud Firestore:** user documents at `users/{uid}` and related subcollections such as `workoutLogs`, `customWorkouts` and `mealLogs`.
- **Firebase Cloud Messaging:** the app registers an FCM token on the user's Firestore document and handles incoming messages through `FitnessMessagingService`.


## GitHub and GitHub Actions

GitHub is used as the source-code repository and collaboration/version-control platform. The repository provides a central place to store the Android Studio project, review changes, maintain project history and share the source with assessors or collaborators.

Recommended repository workflow:

1. Commit a working baseline before making a feature change.
2. Use descriptive commits, for example `Add USDA food search` or `Improve workout category filtering`.
3. Push changes to the GitHub remote repository.
4. Use branches for larger changes and merge them after review and testing.
5. Keep local configuration and credentials out of version control.

### GitHub Actions

GitHub Actions can automate checks whenever code is pushed or a pull request is opened. A suitable Android workflow is to check out the repository, install the configured JDK, set up Gradle and run the Gradle build/tests.

## Setup and running the project

### Requirements

- Android Studio compatible with the project's Android Gradle Plugin and Gradle wrapper.
- JDK 11 or the JDK version required by the installed Android Gradle Plugin.
- Android SDK for the project's compile/target SDK settings.
- A Firebase project with Android app ID `com.example.homefitness`.
- A USDA FoodData Central API key for food search.

## Credits and data sources

- [wger Workout Manager API](https://wger.de/en/software/api)
- [USDA FoodData Central API](https://fdc.nal.usda.gov/api-guide.html)
- [Firebase documentation](https://firebase.google.com/docs)
- [Android Developers](https://developer.android.com/)

## Release history

See [RELEASE_NOTES.md](RELEASE_NOTES.md) for the changes recorded for this project version.
