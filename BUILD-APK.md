# Generate StandbyOS 2.0 APK from GitHub

1. On GitHub, create a NEW repository named `StandbyOS` (the `main` branch is recommended). Do not initialize it with a README because this package already contains one.
2. Add the contents of this archive to the repository root. The top level must contain `app/`, `.github/`, `build.gradle`, `settings.gradle`, `gradle.properties`, and `README.md`.
3. In the repository, navigate to **Actions** and select **Build Android APK**.
4. If GitHub prompts to enable workflows, enable workflows. Click **Run workflow** and select the `main` branch.
5. After the build succeeds, open the run and download the **StandbyOS-2.0-debug-apk** artifact. The APK inside is `app-debug.apk`.

The workflow compiles a debug (test-installable) APK with JDK 17, Android Gradle Plugin 8.7.3, Gradle 8.10.2, and Android SDK 35. Signing is with GitHub's ephemeral debug key; it is NOT a Play Store production release. You may need to uninstall a previously installed version because its signing key differs.

For manual builds, open the project in Android Studio (with Android SDK 35) and choose **Build > Build APK(s)**.
