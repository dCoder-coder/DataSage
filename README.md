# DataSage Android App

DataSage is a modular Android frontend for the RetailIQ backend.

## Configure backend URL

Default API URL for emulator is:

- `http://10.0.2.2:5000/`

Override at build time:

```bash
./gradlew assembleDebug -PAPI_BASE_URL=https://your-retailiq-domain/
```

For persistent local config add this to `~/.gradle/gradle.properties` or project `gradle.properties`:

```properties
API_BASE_URL=https://your-retailiq-domain/
```

The current build URL is visible in **Settings** screen in the app.
