# Release Notes — Home Fitness

## Version 1.0.0 — Project presentation build

This release represents the feature-complete presentation version developed beyond the initial prototype. The notes below describe the features and integrations present in the supplied project source; verify each item on the presentation device before demonstrating it.

### Added and expanded features

- **Account access:** email/password registration and login, Google sign-in integration, and password-reset email flow using Firebase Authentication.
- **Cloud user profiles:** user profile information is stored in Cloud Firestore under the authenticated user's account.
- **Exercise catalogue:** integrated the wger exercise information API using Retrofit and Gson.
- **Workout navigation:** added workout categories, exercise lists, exercise selection and a workout session screen.
- **Custom workouts:** users can assemble custom workouts and save them under their Firestore account.
- **Workout history:** completed workout records are stored in Firestore and used by the progress screen.
- **Progress overview:** added streak and weight metrics, a weekly activity visualisation and recent workout history.
- **Nutrition search:** integrated USDA FoodData Central to search foods and read calorie information from nutrient data.
- **Meal records:** added meal logging under the user's Firestore `mealLogs` collection.
- **Notifications:** added Android notification handling and Firebase Cloud Messaging service integration.
- **Visual refresh:** introduced the Home Fitness logo, green/lime visual styling, updated cards, category shortcuts and revised screen layouts.
- **Local exercise caching:** added a cache for previously retrieved exercise data to improve resilience when the network is unavailable.
- **Developer configuration:** moved the USDA API key input to `local.properties` and generated `BuildConfig` rather than requiring the key in the user interface.

### Design and usability improvements

- Organised the app around home, workouts, nutrition, progress, profile and settings screens.
- Added category shortcuts on the home screen.
- Improved the presentation of workout, nutrition and progress information.
- Added user-facing loading and error messages for remote food searches and exercise retrieval.

### Integrations

- wger REST API for exercise information.
- USDA FoodData Central REST API for food search and nutrient data.
- Firebase Authentication, Cloud Firestore and Firebase Cloud Messaging for account, cloud data and messaging features.

### Known limitations / items to verify

- API availability and returned catalogue content depend on the external providers and network connection.
- Food search requires a valid USDA API key in the developer's local Gradle properties.
- Notification delivery depends on Android notification permission and device settings; FCM delivery also depends on Firebase configuration.
- GitHub Actions automation should only be described as active after a workflow has been committed and a successful run is visible in the repository's Actions tab.
- This release note documents the source project; it is not a claim that every feature has passed automated or device testing.
